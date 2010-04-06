/**
 * DDS - KEYMAP
 * ------------
 * by Gokul Soundararajan
 *
 * Keeps the object meta-data safe.
 *
 **/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

#include <db.h>
#include <err.h>
#include <cmd.h>
#include <keymap.h>

/* For mkdir */
#include <errno.h>
#include <sys/stat.h>
#include <sys/types.h>

/* Environment directory where all the DB stuff is stored */
#define ENV_DIR "./db"

/* A nice default array size for map_list, without growing it */
#define DEFAULT_ARRAY_SIZE 32


/**
 * db_error_handler()
 * ------------------
 * Default error-handling function for DB errors
 *
 **/

void
db_error_handler(const DB_ENV *dbenv, const char *error_prefix, const char *msg)
{
    fprintf(stderr, "%s%s\n", error_prefix, msg);
    /* Print some statistics */
//    dbenv->stat_print((DB_ENV*) dbenv, 0);
}

/**
 * copy_inode()
 * ------------------
 * Make a deep malloc'd copy of an inode_t
 *
 **/

inode_t* copy_inode(inode_t* input)
{
    inode_t* output = malloc(sizeof(inode_t));
    output->object = input->object;
    output->n_locations = input->n_locations;
    int i;
    for (i = 0; i < input->n_locations; i++)
    {
        output->locations[i] = input->locations[i];
    }
    output->ts_put = input->ts_put;
    output->ts_delete = input->ts_delete;

    return output;
}

/**
 * map_init()
 * ----------
 * Initialize your keymap
 *
 **/

map_t* map_init(char *filename) {
    int error;

    /* Make a Berkeley DB environment (needed for anything useful) */
    DB_ENV* new_env;
    error = db_env_create(&new_env, 0);
    if (error != 0) {
        fprintf(stderr, "Error creating environment handle: %s\n",
            db_strerror(error));
        exit(1);
    }

    int env_flags = DB_CREATE |    /* Create the environment */
                DB_INIT_TXN  | /* Initialize transactions */
                DB_INIT_LOCK | /* Initialize locking. */
                DB_INIT_LOG  | /* Initialize logging */
                DB_INIT_MPOOL | /* Initialize the in-memory cache. */
        DB_THREAD;             /* Be re-entrant */

    /* If ENV_DIR doesn't exist, create it */
    errno = 0;
    error = mkdir(ENV_DIR, 0700);
    if (error != 0)
    {
        if (errno != EEXIST)
        {
            fprintf(stderr, "Failed to create DB directory %s\n", ENV_DIR);
            exit (1);
        }
    }

    error = new_env->open(new_env, ENV_DIR, env_flags, 0);
    if ( error != 0 )
    {
        fprintf(stderr, "Environment open failed: %s\n", db_strerror(error));
        exit (1);
    }

    /* Make a new Berkeley DB object */

    DB* new_db = malloc(sizeof(DB));

    error = db_create(&new_db, new_env, 0);
    
    if ( error != 0 )
    {
        fprintf(stderr, "DB creation failed: %s\n", db_strerror(error));
        exit (1);
    }

    /* Register error reporting function */
    new_db->set_errcall(new_db, db_error_handler);
    new_db->set_errpfx(new_db, "DDS DB Error:");

    error = new_db->open(new_db, NULL, filename, NULL, DB_BTREE, DB_CREATE | DB_AUTO_COMMIT, 0665);
    if (error != 0)
    {
        new_db->err(new_db, error, "Database open failed: %s", filename);
    }

    /* Make the map_t to return */
    map_t* map = malloc(sizeof(map_t));
    map->db = new_db;
    map->env = new_env;

    return map;
}

int map_put( map_t *map, obj_t *object, loc_t *locations, int n_locations, unsigned long ts ) {

    /* Declare and clear structures */
    DBT key;
    memset(&key, 0, sizeof(key));
    DBT data;
    memset(&data, 0, sizeof(data));

    /* Set the key */
    key.data = object->key_name;
    key.size = MAX_KEY_NAME_LEN;

    /* Set the data, which will contain a inode_t structure */
    /* Assuming that the data is copied anyway, so it doesn't need to be dynamic memory */
    inode_t db_data;
    db_data.object = *object;
    db_data.n_locations = n_locations;
    int i;
    for (i = 0; i < n_locations; i++)
    {
        db_data.locations[i] = locations[i];
    }
    db_data.ts_put = ts;
    db_data.ts_delete = 0;

    data.data = &db_data;
    data.size = sizeof(inode_t);

    /* Should be auto-transactioned */
    int error = map->db->put(map->db, NULL, &key, &data, 0);
    if (error != 0)
    {
        map->db->err(map->db, error, "Database put failed");
        return error;
    }
    
    return error;
}

int map_get( map_t *map, obj_t *object, loc_t **locations, int *n_locations, int *is_deleted) {
    int error;
    /* I'm assuming that map and object are valid, and the rest need to be filled in */

    /* Declare and clear structures */
    DBT key;
    memset(&key, 0, sizeof(key));
    DBT data;
    memset(&data, 0, sizeof(data));

    /* Set the key */
    key.data = object->key_name;
    key.size = MAX_KEY_NAME_LEN;

    /* Make some memory for the data */
    inode_t gotten_data;
    data.data = &gotten_data;
    data.ulen = sizeof(inode_t);
    data.flags = DB_DBT_USERMEM;

    /* Start a transaction */
    DB_TXN *txn = NULL;
    error = map->env->txn_begin(map->env, NULL, &txn, 0);
    if (error == DB_NOTFOUND)
    {
        /* Not really a database error, so just return an error code */
        return error;
    }
    if (error != 0)
    {
        map->db->err(map->db, error, "Transaction begin failed");
        return error;
    }

    error = map->db->get(map->db, txn, &key, &data, 0);
    if (error != 0)
    {
        map->db->err(map->db, error, "Database get failed");
        txn->abort(txn);
        return error;
    }

    /* End the transaction */
    error = txn->commit(txn, 0);
    if (error != 0)
    {
        map->db->err(map->db, error, "Transaction commit failed");
        return error;
    }

    /* Fill in the provided structures */
    
    /* Locations is expected to be malloc'd */
    *locations = malloc(sizeof(loc_t) * gotten_data.n_locations);
    int i;
    for (i = 0; i < gotten_data.n_locations; i++)
    {
        /* Evidently dereference has too low of an operator precedence */
        (*locations)[i] = gotten_data.locations[i];
    }

    *n_locations = gotten_data.n_locations;
    if (gotten_data.ts_delete > gotten_data.ts_put)
    {
        *is_deleted = 1;
    }
    else
    {
        *is_deleted = 0;
    }

    /* Sanity checking */
    assert(!strcmp(object->key_name, gotten_data.object.key_name));
    assert(!strcmp(object->bucket_name, gotten_data.object.bucket_name));
    
    return error;
}

/**
 * map_del()
 * ---------
 * Implemented as "just update the ts_delete time, don't actually delete the node"
 *
 */
int map_del( map_t *map, obj_t *object, unsigned long ts_delete) {
    int error = 0;

    /* Start a transaction */
    DB_TXN *txn = NULL;
    error = map->env->txn_begin(map->env, NULL, &txn, 0);
    if (error != 0)
    {
        map->db->err(map->db, error, "Transaction begin failed");
        return error;
    }
    
    /**** Get the node *****/
    
    /* Declare and clear structures */
    DBT key;
    memset(&key, 0, sizeof(key));
    DBT data;
    memset(&data, 0, sizeof(data));

    /* Set the key */
    key.data = object->key_name;
    key.size = MAX_KEY_NAME_LEN;

    /* Make some memory for the data */
    inode_t gotten_data;
    data.data = &gotten_data;
    data.ulen = sizeof(inode_t);
    data.flags = DB_DBT_USERMEM;

    /* RMW = Read Modify Write, used since about to put it */
    error = map->db->get(map->db, txn, &key, &data, DB_RMW ); 
    if (error != 0)
    {
        map->db->err(map->db, error, "Database get failed");
        txn->abort(txn);
        return error;
    }
    
    /**** Change its deleted time value *****/
    gotten_data.ts_delete = ts_delete;

    /**** Put it back - should overwrite the current structure ****/
    error = map->db->put(map->db, txn, &key, &data, 0);
    if (error != 0)
    {
        map->db->err(map->db, error, "Database put failed");
        txn->abort(txn);
        return error;
    }

    /* End the transaction */
    error = txn->commit(txn, 0);
    if (error != 0)
    {
        map->db->err(map->db, error, "Transaction commit failed");
        return error;
    }
    
    return error;

    /* If, for some reason, I need to implement an actual delete, here it is */
#ifdef ACTUAL_DELETE

    /* Declare and clear structures */
    DBT key;
    memset(&key, 0, sizeof(key));

    /* Set the key */
    key.data = object->key_name;
    key.size = MAX_KEY_NAME_LEN;
    
    int error = map->db->del(map->db, txn, &key, 0);

    if (error != 0)
    {
        map->db->err(map->db, error, "Database get failed");
        txn->abort(txn);
        return error;
    }
#endif
}

/**
 * map_list()
 * ----------
 * Looks to me like this function is supposed to return all the nodes that
 * are in the given bucket, only showing the "deleted" ones if show_deleted
 * is asserted.
 */
int map_list( map_t *map, obj_t *object, inode_t **nodes, unsigned *n_nodes, int show_deleted ) {
    int error;
    
    /* Start a transaction */
    DB_TXN *txn = NULL;
    error = map->env->txn_begin(map->env, NULL, &txn, 0);
    if (error != 0)
    {
        map->db->err(map->db, error, "Transaction begin failed");
        return error;
    }

    /* Implemented using a cursor, since that seems to be the modern Berkeley DB way */
    DBC* cursorp;
    map->db->cursor(map->db, txn, &cursorp, 0);

    /* Declare and clear structures */
    DBT key;
    memset(&key, 0, sizeof(DBT));
    DBT data;
    memset(&data, 0, sizeof(DBT));

    /* Allocate memory to put the data into */
//    data.ulen = sizeof(inode_t);
//    data.flags = DB_DBT_USERMEM;

    *n_nodes = 0;

    /* Create an array that might be big enough.  We'll resize it later if it's not */
    unsigned int cur_array_size = DEFAULT_ARRAY_SIZE;
    *nodes = malloc(cur_array_size*sizeof(inode_t));


    /* Iterate over the entire set, expanding the array as necessary to fit in new values */
    while ((error = cursorp->get(cursorp, &key, &data, DB_NEXT)) == 0)
    {
        /* Cast what we got out of the database properly */
        inode_t* gotten_data = data.data;

        /* Check if it's the same bucket */
        if (!strcmp(object->bucket_name, gotten_data->object.bucket_name))
        {
            /* Only show deleted ones with show_deleted */
            if (!show_deleted && gotten_data->ts_delete > gotten_data->ts_put)
            {
                continue;
            }
            
            if (*n_nodes > cur_array_size)
            {
                /* Expand the size of the array */
                cur_array_size *= 2;
                *nodes = realloc(*nodes, cur_array_size*sizeof(inode_t));
            }

            /* Copy in the value - remember operator precedence */
            inode_t* copied_inode = copy_inode(gotten_data);
            (*nodes)[*n_nodes] = *copied_inode;
            (*n_nodes)++;
        }
    }
    if (error != DB_NOTFOUND) {
        map->db->err(map->db, error, "Database listall failed");
        txn->abort(txn);
        return error;
    }

    /* Make the *nodes array actually be the right size */
    *nodes = realloc(*nodes, (*n_nodes)*sizeof(inode_t));

    /* Close the cursor - BEFORE the transaction */
    cursorp->close(cursorp);

    /* End the transaction */
    error = txn->commit(txn, 0);
    if (error != 0)
    {
        map->db->err(map->db, error, "Transaction commit failed");
        return error;
    }
    
    return error;
}

/**
 * map_listall()
 * ----------
 * List ALL the nodes stored in the entire keymap
 */

int map_listall( map_t *map, inode_t **nodes, unsigned *n_nodes ) {
    int error;
    
    /* Start a transaction */
    DB_TXN *txn = NULL;
    error = map->env->txn_begin(map->env, NULL, &txn, 0);
    if (error != 0)
    {
        map->db->err(map->db, error, "Transaction begin failed");
        return error;
    }

    /* Implemented using a cursor, since that seems to be the modern Berkeley DB way */
    DBC* cursorp;
    map->db->cursor(map->db, txn, &cursorp, 0);

    /* Declare and clear structures */
    DBT key;
    memset(&key, 0, sizeof(key));
    DBT data;
    memset(&data, 0, sizeof(data));

    /* Allocate memory to put the data into */
    data.ulen = sizeof(inode_t);
    data.flags = DB_DBT_USERMEM;

    *n_nodes = 0;

    /* Iterate over the entire set, just to find out how many there are */
    while ((error = cursorp->get(cursorp, &key, &data, DB_NEXT)) == 0) {
        (*n_nodes)++;
    }
    if (error != DB_NOTFOUND) {
        map->db->err(map->db, error, "Database listall failed");
        txn->abort(txn);
        return error;
    }

    /* Now that we know how many there are, create an array big enough */
    *nodes = malloc(*n_nodes*sizeof(inode_t));

    /* And iterate through again to fill the array */
    unsigned int i;
    for (i = 0,
             error = cursorp->get(cursorp, &key, &data, DB_FIRST);
         (i < *n_nodes)
             && (error == 0);
         i++,
             error = cursorp->get(cursorp, &key, &data, DB_NEXT))
    {
        /* Cast what we got out of the database properly */
        inode_t* gotten_data = data.data;

        /* Copy all the inode_t info into a new one and put it in the array */
        inode_t* copied_inode = copy_inode(gotten_data);
        (*nodes)[i] = *copied_inode;
    }
    if (error != DB_NOTFOUND) {
        map->db->err(map->db, error, "Database listall failed (2nd iteration)");
        txn->abort(txn);
        return error;
    }

    /* Close the cursor - BEFORE the txn*/
    cursorp->close(cursorp);

    /* End the transaction */
    error = txn->commit(txn, 0);
    if (error != 0)
    {
        map->db->err(map->db, error, "Transaction commit failed");
        return error;
    }
    
    return error;
}

/**
 * map_merge()
 * ----------
 * Upon receiving gossip from someone else, go through our keymap,
 * and merge any newer (as identified by the timestamp) nodes
 *
 */
int map_merge( map_t *map, inode_t *nodes, int n_nodes ) {
    int error;
    int i;

    /* Start a transaction */
    DB_TXN *txn = NULL;
    error = map->env->txn_begin(map->env, NULL, &txn, 0);
    if (error != 0)
    {
        map->db->err(map->db, error, "Transaction begin failed");
        return error;
    }

    /* Iterate through all the nodes we've received as gossip */
    for (i = 0; i < n_nodes; i++)
    {
        /* Find the corresponding timestamp in our keymap */

        /* Declare and clear structures */
        DBT key;
        memset(&key, 0, sizeof(key));
        DBT data;
        memset(&data, 0, sizeof(data));

        /* Set the key */
        key.data = nodes[i].object.key_name;
        key.size = MAX_KEY_NAME_LEN;

        /* Make some memory for the data */
        inode_t gotten_data;
        data.data = &gotten_data;
        data.ulen = sizeof(inode_t);
        data.flags = DB_DBT_USERMEM;

        error = map->db->get(map->db, txn, &key, &data, 0);

        if (error == DB_NOTFOUND)
        {
            /* If we didn't find the record, add it */
            data.data = &nodes[i];
            data.size = sizeof(inode_t);
            error = map->db->put(map->db, txn, &key, &data, 0);
            if (error != 0)
            {
                map->db->err(map->db, error, "Database put failed");
                txn->abort(txn);
                return error;
            }

            /* Go to the next one */
            continue;
        }

        /* Some other getting error */
        if (error != 0)
        {
            map->db->err(map->db, error, "Database get failed");
            txn->abort(txn);
            return error;
        }

        /* All right, we found it in the database */

        /* We're using physical timestamps here, and we have four of
         * them.  We need to find out which record (received or
         * current) has the absolute highest timestamp, and then if
         * that's the received one, copy it over */

        if (
            ((nodes[i].ts_put > gotten_data.ts_put) && (nodes[i].ts_put > gotten_data.ts_delete))
            ||
            ((nodes[i].ts_delete > gotten_data.ts_put) && (nodes[i].ts_delete > gotten_data.ts_delete)))
        {
            /* Copy it over */
            data.data = &nodes[i];
            error = map->db->put(map->db, txn, &key, &data, 0);
            if (error != 0)
            {
                map->db->err(map->db, error, "Database put failed");
                txn->abort(txn);
                return error;
            }

            /* Go to the next one */
            continue;
        }
        
        /* Our copy was newer, don't do anything */
    }

    /* End the transaction */
    error = txn->commit(txn, 0);
    if (error != 0)
    {
        map->db->err(map->db, error, "Transaction commit failed");
        return error;
    }

    return error;
}

/* Close the Berkeley DB, ensuring on-disk persistence */
void map_close( map_t *map)
{
    int error = 0;

    if (map->db != NULL) {
        error = map->db->close(map->db, 0);
    }
    if ( error != 0 )
    {
        map->db->err(map->db, error, "Database close failed");
    }
    
    if (map->env != NULL) {
        error = map->env->close(map->env, 0);
    }
    if ( error != 0 )
    {
        map->env->err(map->env, error, "Environment close failed: %s", ENV_DIR);
    }
}
