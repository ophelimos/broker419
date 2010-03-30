/**
 * DDS - Routing
 * -------------
 * by Gokul Soundararajan
 *
 * This sets up the routing policy for
 * dds.
 *
 **/

#include <dds.h>

#define DDS_NODE_LIST_FILE "store.nodes"
#define MIN(a,b) { ( a < b ) ? a : b }

int dds_locate_init( loc_db **db, char *static_nodes_list ) { 

  int erred = 0;
  int i = 0;
  FILE *fd = NULL;
  loc_db *dbp = NULL;

  /* Open Location Database */
  dbp = (loc_db *) malloc( sizeof(loc_db) );
  DIE_IF_EQUAL( (int) dbp, (int) NULL,
		"Could not malloc space for location db",
		cleanup_locate_init,
		&erred );

  /* Initialize Map */
  dbp->length = 0;
  for(i=0; i < STATIC_NODE_LIST_SIZE; i++) {
    memset( &dbp->map[i], 0, sizeof(loc_t) );
  }

  /* Open File */
  fd = fopen( static_nodes_list, "r");
  DIE_IF_EQUAL( (int) fd, (int) NULL, 
		"Could not open storage node list file", 
		cleanup_locate_init, &erred );

  /* Read File and Load List */
  char hostname[256]; int port;
  while( fscanf(fd, "%s %d", hostname, &port) == 2 ) {
    fprintf(stdout, "[NOTE]: Adding storage node: hostname: %s port: %d\n",hostname, port );
    fflush(stdout);
    strncpy(dbp->map[dbp->length].host_name, hostname, MAX_HOST_NAME_LEN-1 );
    dbp->map[dbp->length].port = port;
    dbp->length++;
  }

  /* Finalize */
  *db = dbp;

 cleanup_locate_init:
  if(fd) { fclose(fd); fd = NULL; }
  return erred;
}

int dds_locate_storage_node( loc_db *db, dds_cmd cmd, obj_t *obj, loc_t **locs, int *n_locs ) {

  int erred = 0;
  
  int n_nodes = MIN( 3, db->length ); /* Keep 3 copies, if possible */

  /* Select nodes to place data */
  int i = 0, n_swaps = 10 * db->length;
  int shuffle[STATIC_NODE_LIST_SIZE];
  for(i=0; i < db->length; i++) { shuffle[i] = i; }
  while( n_swaps > 0 ) {
    int x = random() % db->length;
    int y = random() % db->length;
    int t = shuffle[y];
    shuffle[y] = shuffle[x];
    shuffle[x] = t;
    n_swaps--;
  }

  /* Copy */
  loc_t *_locs = NULL;
  _locs = (loc_t *) malloc( n_nodes * sizeof(loc_t) );
  DIE_IF_EQUAL( (int) _locs, (int) NULL, 
		"Could not malloc space for location list",
		cleanup_locate_storage_node,
		&erred );

  for(i=0; i < n_nodes; i++) {
    _locs[i] = db->map[shuffle[i]];
  }


  /* Finalize */
  *n_locs = n_nodes;
  *locs = _locs;

 cleanup_locate_storage_node:
  if(erred && *locs ) { free(*locs); *locs = NULL; }
  if(erred) { *n_locs = 0; }

  return erred;

}

#ifdef ROUTE_TEST

int main(int argc, char **argv ) {

  loc_db *db = NULL;
  dds_locate_init( &db );

  loc_t *loc_list; 
  int loc_list_len;
  int i = 0;

  while(1) {
    fprintf(stdout, "\n");
    dds_locate_storage_node( db, DDS_PUT, NULL, &loc_list, &loc_list_len );
    for(i=0; i < loc_list_len; i++) {
      fprintf(stdout, "Hostname: %s Port: %d\n", loc_list[i].host_name, loc_list[i].port );
    }
    free(loc_list);
  }
  return 0;

}

#endif
