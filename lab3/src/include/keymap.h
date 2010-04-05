/**
 * DDS - KEYMAP
 * ------------
 * by Gokul Soundararajan
 *
 * Maintains the meta-data for DDS
 *
 **/

#ifndef __KEYMAP_H__
#define __KEYMAP_H__

#include <stdlib.h>
#include <pthread.h>
#include <types.h>

#define KEYMAP_FLUSH_FREQUENCY 1000

struct map_t;

typedef struct map_t {
  DB *db;
    DB_ENV *env;
    /* I'm using transactions, so I don't need to use a pthread mutex */
/*  pthread_mutex_t lock;*/
} map_t;

map_t* map_init(char *filename);
int    map_put(map_t *map, obj_t *object, loc_t *locations, int n_locations, unsigned long ts );
int    map_list(map_t *map, obj_t *object, inode_t **nodes, unsigned *n_nodes, int show_deleted );
int    map_listall(map_t *map, inode_t **nodes, unsigned *n_nodes );
int    map_get(map_t *map, obj_t *object, loc_t **locations, int *n_locations, int *is_deleted);
int    map_del(map_t *map, obj_t *object, unsigned long ts_delete );
int    map_merge( map_t *map, inode_t *nodes, int n_nodes );
void   map_close( map_t *map );

#endif
