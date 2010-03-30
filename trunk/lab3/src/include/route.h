/**
 * DDS - ROUTE
 * -----------
 * by Gokul Soundararajan
 *
 * Separated routing into its own file
 * to minimize code size
 *
 **/

#ifndef __ROUTE_H__
#define __ROUTE_H__

#include <err.h>
#include <types.h>

#define STATIC_NODE_LIST_SIZE 1024

typedef struct loc_db {  
  struct loc_t map[STATIC_NODE_LIST_SIZE];
  int length;
} loc_db;

int dds_locate_init(loc_db **db, char *static_nodes_list );
int dds_locate_storage_node( loc_db *db, dds_cmd cmd, obj_t *obj, loc_t **locs, int *n_locs);


#endif
