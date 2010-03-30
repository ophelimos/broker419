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

/**
 * map_init()
 * ----------
 * Initialize your keymap
 *
 **/

map_t* map_init(char *filename) {

  /* TODO: Implement this */
  return NULL;

}

int map_put( map_t *map, obj_t *object, loc_t *locations, int n_locations, unsigned long ts ) {

  /* TODO: Implement this */
  return -1;
}



int map_get( map_t *map, obj_t *object, loc_t **locations, int *n_locations, int *is_deleted) {

  /* TODO: Implement this */
  return -1;

}

int map_del( map_t *map, obj_t *object, unsigned long ts_delete) {

  /* TODO: Implement this */
  return -1;

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




