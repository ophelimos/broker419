/**
 * DDS - TYPES
 * -----------
 * by Gokul Soundararajan
 *
 * Data types used by DDS
 *
 **/

#ifndef __TYPES_H__
#define __TYPES_H__


#define MAX_BUCKET_NAME_LEN 512
#define MAX_KEY_NAME_LEN 512

typedef struct obj_t {
  char bucket_name[MAX_BUCKET_NAME_LEN]; 
  char key_name[MAX_KEY_NAME_LEN];
} obj_t;

#define MAX_HOST_NAME_LEN 512

typedef struct loc_t {
  char host_name[MAX_HOST_NAME_LEN];
  int port;
} loc_t;

#define INODE_MAX_LOCATIONS 8
#define TIMESTAMP_MAX_DDS 4

typedef struct inode_t {
  obj_t object;
  int n_locations;
  loc_t locations[INODE_MAX_LOCATIONS];
    /* These are the only timestamps I'm going to need, since I'm
     * implementing coherency, not locking: if there's a race, the
     * latest person wins */
  unsigned long ts_delete;
  unsigned long ts_put;
} inode_t;

#endif
