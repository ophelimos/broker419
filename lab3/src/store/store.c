/**
 * DDS - STORE
 * -----------
 * by Gokul Soundararajan
 *
 * This provides the underlying storage
 * on one machine.
 * 
 **/

#include <dds.h>

/* Global Variables */
/* ----------------------------------------------------------------- */
static char *work_dir;
static int port;

static abacus *ab;
static FILE *fd_stats;

/* Abacus Events */
enum dds_store_events {
  EVT_STORE_MSG = 0,
  EVT_STORE_PUT,
  EVT_STORE_GET,
  EVT_STORE_DELETE,
  EVT_STORE_LIST,
  EVT_STORE_MAX_EVENT /* keep this last */
};

enum dds_store_tasks {
  TSK_STORE_MSG = 0,
  TSK_STORE_PUT,
  TSK_STORE_GET,
  TSK_STORE_DELETE,
  TSK_STORE_LIST,
  TSK_STORE_MAX_TASK /* keep this last */
};

/* Function Headers */
/* ----------------------------------------------------------------- */

/* Hermes Adapters */
static int __dds_validate( hms_endpoint *endpoint, hms_msg *msg );
static int __dds_accepts(  hms_endpoint *endpoint, hms_msg *msg );
static int __dds_handle(   hms_endpoint *endpoint, hms_msg *msg );

/* DDS Functions */
static int __dds_find_verb( char *verb );

/* Verb Handlers */
static int __dds_handle_get( hms_endpoint *endpoint, hms_msg *msg );
static int __dds_handle_put( hms_endpoint *endpoint, hms_msg *msg );
static int __dds_handle_delete( hms_endpoint *endpoint, hms_msg *msg );
static int __dds_handle_list( hms_endpoint *endpoint, hms_msg *msg );

/* Stats */
static int __dds_dump_stats(FILE *fd);

/* Main */
/* ----------------------------------------------------------------- */

int main(int argc, char **argv) {

  hms_ops ops;
  hms *hermes = NULL;

  /* parse command line arguments */
  if( argc == 3 ) {
    port = atoi( argv[1] );
    work_dir = argv[2];
  } else {
    fprintf(stderr, "USAGE: %s <port> <working-dir>\n", argv[0] );
    fflush(stderr);
    return -1;
  }

  /* setup hermes handlers */
  ops.hms_handle = __dds_handle;
  ops.hms_validate = __dds_validate;
  ops.hms_accepts = __dds_accepts;

  /* start-up abacus */
  {
    char stats_filename[512]; struct utsname hostname;
    ab = abacus_init( TSK_STORE_MAX_TASK, EVT_STORE_MAX_EVENT, 1 );
    uname( &hostname );
    sprintf( stats_filename, "%s_%s_%d.stats", "store", hostname.nodename, port );
    fd_stats = fopen( stats_filename, "w" );
    if(!fd_stats) { 
      fprintf(stderr, "[ERROR]: Could not open file to dump stats!\n" );
      fflush(stderr);
      return -1; 
    }
  }

  /* start-up hermes */
  hermes = hermes_init(1, port, ops );

  /* do nothing */
  /* hermes runs on its own */
  while(1) { 

    /* Dump stats */
    __dds_dump_stats( fd_stats );

    sleep(10);
  }

  hermes_shutdown( hermes, HMS_TRUE );

  return 0;

}

/* Function Implementation */
/* ----------------------------------------------------------------- */

static int __dds_dump_stats( FILE *fd ) {

  int ret = 0, erred = 0;

  /* Check input */
  DIE_IF_EQUAL( (int) fd, (int) NULL, "fd is NULL", cleanup, &erred );
  DIE_IF_EQUAL( (int) ab, (int) NULL, "ab is NULL", cleanup, &erred );

  /* NOTE: Assuming single threaded flusher */
  if( abacus_event_periodall(ab) > 10000 ) {

    unsigned n_events = abacus_event_count(ab, EVT_STORE_MSG, 0);
    unsigned n_puts = abacus_event_count(ab, EVT_STORE_PUT, 0);
    unsigned n_gets = abacus_event_count(ab, EVT_STORE_GET, 0);
    unsigned n_deletes = abacus_event_count(ab, EVT_STORE_DELETE, 0);
    unsigned n_lists = abacus_event_count(ab, EVT_STORE_LIST, 0);

    double   l_events = abacus_task_avgdelay(ab, TSK_STORE_MSG, 0);
    double   l_puts = abacus_task_avgdelay(ab, TSK_STORE_PUT, 0);
    double   l_gets = abacus_task_avgdelay(ab, TSK_STORE_GET, 0);
    double   l_deletes = abacus_task_avgdelay(ab, TSK_STORE_DELETE, 0);
    double   l_lists = abacus_task_avgdelay(ab, TSK_STORE_LIST, 0);

    fprintf(fd, "TIME: %10.3lf N_EVTS: %5u N_PUTS: %5u N_GETS: %5u N_DELETES: %5u N_LISTS: %5u L_EVTS: %7.3lf L_PUTS: %7.3lf L_GETS: %7.3lf L_DELETES: %7.3lf L_LISTS: %7.3lf\n",
	    abacus_time(ab),
	    n_events, n_puts, n_gets, n_deletes, n_lists,
	    l_events, l_puts, l_gets, l_deletes, l_lists
	    );
    fflush(fd);

    /* Reset Counts */
    abacus_resetall(ab);

  }


 cleanup:
  return erred;

}

static int __dds_find_verb( char *verb ) {

  int i = 0;
  for(i=0; i < DDS_MAX; i++ ) {
    if( strcasecmp( verb, dds_verbs[i] ) == 0) {
      return i;
    }
  }

  return DDS_MAX;

} /* end __dds_find_verb() */


static int __dds_validate( hms_endpoint *endpoint, hms_msg *msg ) {

  return 0; /* everything is valid */

}


static int __dds_accepts(  hms_endpoint *endpoint, hms_msg *msg ) {

  int ret = 0, erred = 0;
  char *verb = NULL;
  int will_accept;

  ret = hms_msg_get_verb( msg, &verb );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not get verb from message", cleanup, &erred );

  if( __dds_find_verb( verb ) >= 0 && __dds_find_verb( verb ) < DDS_MAX ) {
    will_accept = 0;
  } else { will_accept = -1; }
  
 cleanup:
  if(verb) { free(verb); verb = NULL; }

  return erred || will_accept;

} 


static int __dds_handle(   hms_endpoint *endpoint, hms_msg *msg ) {

  int ret = 0, erred = 0;
  char *verb = NULL;
  guid_t *id = NULL;

  /* Record event in abacus */
  id = guid_create();
  DIE_IF_EQUAL( (int) id, (int) NULL, "Could not create guid", cleanup, &erred );
  ret = abacus_event_add( ab, EVT_STORE_MSG, 0 );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not log event into abacus", cleanup, &erred );
  ret = abacus_task_add( ab, id );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not add task into abacus", cleanup, &erred );

  /* Get verb */
  ret = hms_msg_get_verb( msg, &verb );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not get verb from message", cleanup, &erred );

  /* call different verb handler */
  int vid = __dds_find_verb( verb );
  switch( vid ) {
  case  DDS_GET:
    ret = __dds_handle_get( endpoint, msg );
    break;
  case DDS_PUT:
    ret = __dds_handle_put( endpoint, msg );
    break;
  case DDS_DELETE:
    ret = __dds_handle_delete( endpoint, msg );
    break;
  case DDS_LISTBUCKET:
    ret = __dds_handle_list( endpoint, msg );
    break;
  default:
    fprintf(stdout, "No verb handler for [%s]\n", verb );
    break;
  }

  /* Delete task from abacus */
  ret = abacus_task_delete(ab, id);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not delete task abacus", cleanup, &erred );
  guid_destroy( id ); id = NULL;

 cleanup:
  if(ret) {
    fprintf(stderr, "[ERROR]: Could not handle message\n", msg );
    fflush(stderr);
  }
  if(verb) { free(verb); verb = NULL; }
  if(id) { guid_destroy(id); id = NULL; }
  return ret; /* everything ok */

}


/* Verb Handlers */
/* ----------------------------------------------------------------- */

static int __dds_handle_get( hms_endpoint *endpoint, hms_msg *msg ) {

  int ret = 0, ret2 = 0, ret3 = 0, erred = 0;
  char *bucket_name = NULL, *key_name = NULL, filename[1024];
  
  FILE *fd = NULL;
  int file_len = -1;
  char *buffer = NULL;
  hms_msg *reply = NULL;
  char status_code[9];
  guid_t *id = NULL;

  /* Record event in abacus */
  ret = abacus_event_add( ab, EVT_STORE_GET, 0 );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not record event into abacus", err, &erred );

  /* Start task */
  id = guid_create();
  DIE_IF_EQUAL( (int) id, (int) NULL, "Could not create guid", err, &erred );
  ret = abacus_task_add( ab, id );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not add task into abacus", err, &erred );
  ret = abacus_task_start( ab, id, TSK_STORE_GET, 0);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not add task into abacus", err, &erred );

  /* Extract Bucket and File */
  hms_msg_get_named_header( msg , "Bucket", &bucket_name );
  hms_msg_get_named_header( msg,  "Key", &key_name );

  /* Check if the values are correct */
  DIE_IF_EQUAL( (int) bucket_name, (int) NULL, "No bucket specified", err, &erred );
  DIE_IF_EQUAL( (int) key_name, (int) NULL, "No key specified", err, &erred );

  /* Open file and return contents */
  /* Compose filename */
  sprintf( filename, "%s/%s/%s", work_dir, bucket_name, key_name );

  /* Open file */
  fd = fopen( filename, "rb" );
  DIE_IF_EQUAL( (int) fd, (int) NULL, "Could not open file corresponding to object", err, &erred );  
  
  /* Find file length */
  ret = fseek( fd, 0, SEEK_END ); file_len = ftell(fd); ret2 = fseek( fd, 0L, SEEK_SET );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not seek to end of file", err, &erred );  
  DIE_IF_EQUAL( file_len, -1, "Could not tell length of file", err, &erred );  
  DIE_IF_NOT_EQUAL( ret2, 0, "Could not seek to beginning of file", err, &erred );  

  /* Allocate buffer */
  buffer = ( char * ) malloc( file_len + 1 );
  DIE_IF_EQUAL( (int) buffer, (int) NULL, "Could malloc buffer for file", err, &erred );  
  
  /* Read the data */
  ret = fread( buffer, file_len, 1, fd );
  DIE_IF_NOT_EQUAL( ret, 1, "Could not read entire file", err, &erred );  

  /* Close the file */
  ret = fclose( fd ); fd = NULL;
  DIE_IF_NOT_EQUAL( ret, 0, "Could not close file", err, &erred );  
  
  /* Reply with the data */
  reply = hms_msg_create();
  DIE_IF_EQUAL( (int) reply, (int) NULL, "Could not create reply message", err, &erred );  
  ret = hms_msg_set_verb(reply,"OK");
  DIE_IF_NOT_EQUAL( ret, 0, "Could not add verb", err, &erred );  
  ret = hms_msg_add_named_header( reply, "Bucket", bucket_name );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not add Bucket header", err, &erred );  
  ret = hms_msg_add_named_header( reply, "Key", key_name );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not add Key header", err, &erred );  
  ret = hms_msg_set_body( reply, buffer, file_len );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not add message body", err, &erred );  
  ret = hms_endpoint_send_msg( endpoint, reply );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not send reply", err, &erred );  
  ret = hms_msg_destroy( reply ); reply = NULL;
  DIE_IF_NOT_EQUAL( ret, 0, "Could not destroy reply", err, &erred );  

  err:

  /* Record end into abacus */
  ret = abacus_task_end(ab, id, TSK_STORE_GET, 0);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not delete task abacus", cleanup, &erred );
  ret = abacus_task_delete(ab, id);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not delete task abacus", cleanup, &erred );
  guid_destroy( id ); id = NULL;

  /* Send an error message back */
  if( erred ) {
    reply = hms_msg_create();
    DIE_IF_EQUAL( (int) reply, (int) NULL, "Could not create reply message", cleanup, &erred );  
    ret = hms_msg_set_verb(reply,"ERROR");
    DIE_IF_NOT_EQUAL( ret, 0, "Could not add verb", cleanup, &erred ); 
    if( bucket_name && key_name ) {
      ret = hms_msg_add_named_header( reply, "Bucket", bucket_name );
      DIE_IF_NOT_EQUAL( ret, 0, "Could not add Bucket header", cleanup, &erred );  
      ret = hms_msg_add_named_header( reply, "Key", key_name );
      DIE_IF_NOT_EQUAL( ret, 0, "Could not add Key header", cleanup, &erred );  
    }
    ret = hms_endpoint_send_msg( endpoint, reply );
    DIE_IF_NOT_EQUAL( ret, 0, "Could not send reply", cleanup, &erred );
    ret = hms_msg_destroy( reply ); reply = NULL;
    DIE_IF_NOT_EQUAL( ret, 0, "Could not destroy reply", cleanup, &erred );    
  }

  /* Free before leaving */
 cleanup:  
  if(fd) { fclose(fd); fd = NULL; }
  if( bucket_name ) { free(bucket_name); bucket_name = NULL; }
  if( key_name ) { free(key_name); key_name = NULL; }
  if( reply ) { hms_msg_destroy( reply ); reply = NULL; }
  if( buffer) { free(buffer); buffer = NULL; }
  if( id ) { guid_destroy( id ); id = NULL; }
  return erred;

}

static int __dds_handle_put( hms_endpoint *endpoint, hms_msg *msg ) {

  int erred = 0;
  char *bucket_name = NULL, *key_name = NULL, filename[512];
  
  int ret = 0;
  FILE *fd = NULL;
  int file_len = -1;
  char *buffer = NULL;
  hms_msg *reply = NULL;
  char status_code[9];
  guid_t *id = NULL;

  /* Record event in abacus */
  ret = abacus_event_add( ab, EVT_STORE_PUT, 0 );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not log event into abacus", err, &erred );

  /* Start task */
  id = guid_create();
  DIE_IF_EQUAL( (int) id, (int) NULL, "Could not create guid", err, &erred );
  ret = abacus_task_add( ab, id );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not add task into abacus", err, &erred );
  ret = abacus_task_start( ab, id, TSK_STORE_PUT, 0);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not start task in abacus", err, &erred );

  /* Extract Bucket and File */
  hms_msg_get_named_header( msg , "Bucket", &bucket_name );
  hms_msg_get_named_header( msg,  "Key", &key_name );

  /* Check if the values are correct */
  DIE_IF_EQUAL( (int) bucket_name, (int) NULL, "No bucket provided", err, &erred );
  DIE_IF_EQUAL( (int) key_name, (int) NULL, "No key provided", err, &erred );

  /* Open file and return contents */
  /* Make the directory */
  sprintf( filename, "%s/%s", work_dir, bucket_name );
  ret = mkdir( filename, S_IRWXU );
  DIE_IF_EQUAL( (int) (ret != 0 && errno != EEXIST), 1, "Could not create bucket", err, &erred );

  /* Compose filename */
  sprintf( filename, "%s/%s/%s", work_dir, bucket_name, key_name );
  
  /* Open file */
  fd = fopen( filename, "wb" );
  DIE_IF_EQUAL( fd, NULL, "Could not open file for writing", err, &erred );
  
  /* Allocate buffer */
  file_len = hms_msg_get_body_size(msg);
  DIE_IF_EQUAL( file_len, 0, "Content-Length was 0", err, &erred );
  ret = hms_msg_get_body( msg, &buffer, &file_len );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not get message body", err, &erred );
  DIE_IF_EQUAL( (int) buffer, (int) NULL, "Could not get message body", err, &erred );
  
  /* Read the data */
  ret = fwrite( buffer, file_len, 1, fd );
  DIE_IF_NOT_EQUAL( ret, 1, "Could not write data to file", err, &erred );
  ret = fflush( fd );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not flush contents to disk", err, &erred );  
  ret = fclose( fd ); fd = NULL;
  DIE_IF_NOT_EQUAL( ret, 0, "Could not close file", err, &erred );  
  
  /* Reply with the data */
  reply = hms_msg_create();
  DIE_IF_EQUAL( (int) reply, (int) NULL, "Could not create reply message", err, &erred );  
  ret = hms_msg_set_verb(reply,"OK");
  DIE_IF_NOT_EQUAL( ret, 0, "Could not add verb", err, &erred );  
  ret = hms_msg_add_named_header( reply, "Bucket", bucket_name );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not add Bucket header", err, &erred );  
  ret = hms_msg_add_named_header( reply, "Key", key_name );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not add Key header", err, &erred );  
  ret = hms_endpoint_send_msg( endpoint, reply );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not send reply", err, &erred );  
  ret = hms_msg_destroy( reply ); reply = NULL;
  DIE_IF_NOT_EQUAL( ret, 0, "Could not destroy reply", err, &erred );  
  

 err:

  /* Record end into abacus */
  ret = abacus_task_end(ab, id, TSK_STORE_PUT, 0);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not delete task abacus", cleanup, &erred );
  ret = abacus_task_delete(ab, id);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not delete task abacus", cleanup, &erred );
  guid_destroy( id ); id = NULL;

  /* Reply with error message */
  if( erred ) {
    reply = hms_msg_create();
    DIE_IF_EQUAL( (int) reply, (int) NULL, "Could not create reply message", cleanup, &erred );  
    ret = hms_msg_set_verb(reply,"ERROR");
    DIE_IF_NOT_EQUAL( ret, 0, "Could not add verb", cleanup, &erred ); 
    if( bucket_name && key_name ) {
      ret = hms_msg_add_named_header( reply, "Bucket", bucket_name );
      DIE_IF_NOT_EQUAL( ret, 0, "Could not add Bucket header", cleanup, &erred );  
      ret = hms_msg_add_named_header( reply, "Key", key_name );
      DIE_IF_NOT_EQUAL( ret, 0, "Could not add Key header", cleanup, &erred );  
    }
    ret = hms_endpoint_send_msg( endpoint, reply );
    DIE_IF_NOT_EQUAL( ret, 0, "Could not send reply", cleanup, &erred );
    ret = hms_msg_destroy( reply ); reply = NULL;
    DIE_IF_NOT_EQUAL( ret, 0, "Could not destroy reply", cleanup, &erred );    
  }

 cleanup:  
  /* free before leaving */
  if(fd) { fclose(fd); fd = NULL; }
  if( bucket_name ) { free(bucket_name); bucket_name = NULL; }
  if( key_name ) { free(key_name); key_name = NULL; }
  if( reply ) { hms_msg_destroy( reply ); reply = NULL; }
  if( buffer) { free(buffer); buffer = NULL; }
  if(id) { guid_destroy(id); id = NULL; }

  return erred;

}

static int __dds_handle_delete( hms_endpoint *endpoint, hms_msg *msg ) {


  int ret = 0, erred = 0;
  char *bucket_name = NULL, *key_name = NULL, filename[1024];
  
  hms_msg *reply = NULL;
  char status_code[9];
  guid_t *id = NULL;

  /* Record event in abacus */
  ret = abacus_event_add( ab, EVT_STORE_DELETE, 0 );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not record event into abacus", cleanup, &erred );

  /* Start task */
  id = guid_create();
  DIE_IF_EQUAL( (int) id, (int) NULL, "Could not create guid", err, &erred );
  ret = abacus_task_add( ab, id );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not add task into abacus", err, &erred );
  ret = abacus_task_start( ab, id, TSK_STORE_DELETE, 0);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not start task in abacus", err, &erred );

  /* Extract Bucket and File */
  hms_msg_get_named_header( msg , "Bucket", &bucket_name );
  hms_msg_get_named_header( msg,  "Key", &key_name );

  /* Check if the values are correct */
  DIE_IF_EQUAL( (int) bucket_name, (int) NULL, "No bucket provided", err, &erred );
  DIE_IF_EQUAL( (int) key_name, (int) NULL, "No key provided", err, &erred );

  /* Compose filename */
  sprintf( filename, "%s/%s/%s", work_dir, bucket_name, key_name );

  /* Delete it */
  ret = remove(filename);
  DIE_IF_EQUAL( (int) (ret != 0 && errno != ENOENT), 1, "Could not delete file", err, &erred );
  
  /* Reply with the data */
  reply = hms_msg_create();
  DIE_IF_EQUAL( (int) reply, (int) NULL, "Could not create reply message", err, &erred );  
  ret = hms_msg_set_verb(reply,"OK");
  DIE_IF_NOT_EQUAL( ret, 0, "Could not add verb", err, &erred );  
  ret = hms_msg_add_named_header( reply, "Bucket", bucket_name );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not add Bucket header", err, &erred );  
  ret = hms_msg_add_named_header( reply, "Key", key_name );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not add Key header", err, &erred );  
  ret = hms_endpoint_send_msg( endpoint, reply );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not send reply", err, &erred );  
  ret = hms_msg_destroy( reply ); reply = NULL;
  DIE_IF_NOT_EQUAL( ret, 0, "Could not destroy reply", err, &erred );  


 err:

  /* Record end into abacus */
  ret = abacus_task_end(ab, id, TSK_STORE_DELETE, 0);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not delete task abacus", cleanup, &erred );
  ret = abacus_task_delete(ab, id);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not delete task abacus", cleanup, &erred );
  guid_destroy( id ); id = NULL;

  /* Return error message */
  if( erred ) {
    reply = hms_msg_create();
    DIE_IF_EQUAL( (int) reply, (int) NULL, "Could not create reply message", cleanup, &erred );  
    ret = hms_msg_set_verb(reply,"ERROR");
    DIE_IF_NOT_EQUAL( ret, 0, "Could not add verb", cleanup, &erred ); 
    if( bucket_name && key_name ) {
      ret = hms_msg_add_named_header( reply, "Bucket", bucket_name );
      DIE_IF_NOT_EQUAL( ret, 0, "Could not add Bucket header", cleanup, &erred );  
      ret = hms_msg_add_named_header( reply, "Key", key_name );
      DIE_IF_NOT_EQUAL( ret, 0, "Could not add Key header", cleanup, &erred );  
    }
    ret = hms_endpoint_send_msg( endpoint, reply );
    DIE_IF_NOT_EQUAL( ret, 0, "Could not send reply", cleanup, &erred );
    ret = hms_msg_destroy( reply ); reply = NULL;
    DIE_IF_NOT_EQUAL( ret, 0, "Could not destroy reply", cleanup, &erred );    
  }

  /* Free before leaving */  
 cleanup:  
  if( bucket_name ) { free(bucket_name); bucket_name = NULL; }
  if( key_name ) { free(key_name); key_name = NULL; }
  if( reply ) { hms_msg_destroy( reply ); reply = NULL; }
  if( id ) { guid_destroy( id ); id = NULL; }

  return erred;
  

}

static int __dds_handle_list( hms_endpoint *endpoint, hms_msg *msg ) {

  int i = 0;
  int ret = 0, erred = 0;
  char *bucket_name;
  char dir_name[1024];

  int n;
  struct dirent **file_list = NULL;
  char *list_buf = NULL;     int buf_len = 0;
  hms_msg *reply = NULL;
  guid_t *id = NULL;

  /* Record event in abacus */
  ret = abacus_event_add( ab, EVT_STORE_LIST, 0 );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not record event into abacus", cleanup, &erred );

  /* Start task */
  id = guid_create();
  DIE_IF_EQUAL( (int) id, (int) NULL, "Could not create guid", err, &erred );
  ret = abacus_task_add( ab, id );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not add task into abacus", err, &erred );
  ret = abacus_task_start( ab, id, TSK_STORE_LIST, 0);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not start task in abacus", err, &erred );

  /* Extract bucket name (if given) */
  hms_msg_get_named_header( msg, "Bucket", &bucket_name );

  /* Compose Directory name */
  DIE_IF_EQUAL( (int) bucket_name, (int) NULL, "No bucket provided", err, &erred );
  sprintf(dir_name, "%s/%s", work_dir, bucket_name );
  
  /* Scan the directory */
  n = scandir( dir_name, &file_list, 0, alphasort);
  DIE_IF_EQUAL( n, -1, "Could not scan underlying directory", err, &erred );
  
  /* Calculate buffer to store list */
  for(i=0; i < n; i++) { 
    if(strcmp(file_list[i]->d_name, ".") != 0 && strcmp(file_list[i]->d_name, "..") != 0) {
      buf_len += strlen( file_list[i]->d_name ) + 1; /* "\n" sep */
    }
  }
  buf_len++; /* for "\0" */
  
  /* Make list */
  char *p = NULL; 
  list_buf = (char *) malloc( buf_len );
  DIE_IF_EQUAL( (int) list_buf, (int) NULL, "Could not malloc space to keep list", err, &erred );

  /* Compose Reply */
  p = list_buf;
  for(i=0; i < n; i++) {
    if(strcmp(file_list[i]->d_name, ".") != 0 && strcmp(file_list[i]->d_name, "..") != 0) {
      sprintf(p, "%s\n", file_list[i]->d_name );
      p += (strlen(file_list[i]->d_name) + 1);
    }
  }
  list_buf[buf_len-1] = '\0';
  
  /* Reply with the data */
  reply = hms_msg_create();
  DIE_IF_EQUAL( (int) reply, (int) NULL, "Could not create reply message", err, &erred );  
  ret = hms_msg_set_verb(reply,"OK");
  DIE_IF_NOT_EQUAL( ret, 0, "Could not add verb", err, &erred );  
  ret = hms_msg_add_named_header( reply, "Bucket", bucket_name );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not add Bucket header", err, &erred );  
  ret = hms_msg_set_body( reply, list_buf, buf_len );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not add message body", err, &erred );  
  ret = hms_endpoint_send_msg( endpoint, reply );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not send reply", err, &erred );  
  ret = hms_msg_destroy( reply ); reply = NULL;
  DIE_IF_NOT_EQUAL( ret, 0, "Could not destroy reply", err, &erred );  


 err:

  /* Record end into abacus */
  ret = abacus_task_end(ab, id, TSK_STORE_LIST, 0);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not delete task abacus", cleanup, &erred );
  ret = abacus_task_delete(ab, id);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not delete task abacus", cleanup, &erred );
  guid_destroy( id ); id = NULL;

  /* Send error reply */
  if( erred ) {
    reply = hms_msg_create();
    DIE_IF_EQUAL( (int) reply, (int) NULL, "Could not create reply message", cleanup, &erred );  
    ret = hms_msg_set_verb(reply,"ERROR");
    DIE_IF_NOT_EQUAL( ret, 0, "Could not add verb", cleanup, &erred ); 
    if( bucket_name ) {
      ret = hms_msg_add_named_header( reply, "Bucket", bucket_name );
      DIE_IF_NOT_EQUAL( ret, 0, "Could not add Bucket header", cleanup, &erred );  
    }
    ret = hms_endpoint_send_msg( endpoint, reply );
    DIE_IF_NOT_EQUAL( ret, 0, "Could not send reply", cleanup, &erred );
    ret = hms_msg_destroy( reply ); reply = NULL;
    DIE_IF_NOT_EQUAL( ret, 0, "Could not destroy reply", cleanup, &erred );    
  }

  /* Free before leaving */
 cleanup:
  if(file_list) { for(i=0; i < n; i++) { free(file_list[i]); file_list[i] = NULL; } free(file_list); }
  if(list_buf) { free(list_buf); list_buf = NULL; }
  if(bucket_name) { free(bucket_name); bucket_name = NULL; }
  if(reply) { hms_msg_destroy(reply); reply = NULL; }
  if(id) { guid_destroy(id); id = NULL; }

  return erred;

} /* end __dds_handle_list() */
