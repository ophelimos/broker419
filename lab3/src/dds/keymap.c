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

/* Environment directory where all the DB stuff is stored */
#define ENV_DIR "."


/**
 * db_error_handler()
 * ------------------
 * Default error-handling function for DB errors
 *
 **/

void
db_error_handler(const DB_ENV *dbenv, const char *error_prefix, const char *msg)
{
    fprintf(stderr, "%s%s", error_prefix, msg);
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
    error = new_env->open(new_env, ENV_DIR, DB_CREATE | DB_INIT_MPOOL, 0);
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

    error = new_db->open(new_db, NULL, filename, NULL, DB_BTREE, DB_CREATE, 0665);
    if (error != 0)
    {
        new_db->err(new_db, error, "Database open failed: %s", filename);
    }

    /* Make the map_t to return */
    map_t* map = malloc(sizeof(map_t));
    map->db = new_db;
    map->env = new_env;
    /* Initialize the lock? */

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
    
    int error = map->db->put(map->db, NULL, &key, &data, 0);
    if (error != 0)
    {
        map->db->err(map->db, error, "Database put failed");
        return error;
    }
    
    return error;
}



int map_get( map_t *map, obj_t *object, loc_t **locations, int *n_locations, int *is_deleted) {

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
    /* data.size is set automatically */

    int error = map->db->get(map->db, NULL, &key, &data, 0);
    if (error != 0)
    {
        map->db->err(map->db, error, "Database get failed");
        return error;
    }

    /* Fill in the provided structures */
    
    /* Locations is expected to be malloc'd */
    *locations = malloc(sizeof(loc_t) * gotten_data.n_locations);
    int i;
    for (i = 0; i < gotten_data.n_locations; i++)
    {
        *locations[i] = gotten_data.locations[i];
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
    
    return error;
}

int map_del( map_t *map, obj_t *object, unsigned long ts_delete) {

    /* This one I'm not totally sure about.  I'm going to implement it
     * as an actual delete, but we might at some point want it just to
     * be an update of the delete time */

    /* Declare and clear structures */
    DBT key;
    memset(&key, 0, sizeof(key));

    /* Set the key */
    key.data = object->key_name;
    key.size = MAX_KEY_NAME_LEN;
    
    int error = map->db->del(map->db, NULL, &key, 0);

    if (error != 0)
    {
        map->db->err(map->db, error, "Database get failed");
        return error;
    }
    
    return error;
}

int map_list( map_t *map, obj_t *object, inode_t **nodes, unsigned *n_nodes, int show_deleted ) {

  /* TODO: Implement this */
  return -1;

}

int map_listall( map_t *map, inode_t **nodes, unsigned *n_nodes ) {

  /* TODO: Implement this */
  return -1;
}

int map_merge( map_t *map, inode_t *nodes, int n_nodes ) {

  /* TODO: Implement this */
  return -1;

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
