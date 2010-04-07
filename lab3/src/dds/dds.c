/**
 * DDS
 * ---
 * by Gokul Soundararajan
 *
 * This unifies several stores
 * on one machine.
 * 
 * NOTE: the list is updated as follows:
 *
 * whenever someone sends me a request for hte list, i check if their name is in the list
 * if their name is not in list, then i add them, else ill just give them the list
 *
 * TODO: remove them from the list when they go down
 **/

#include <dds.h>
#include <sys/time.h>
#include <stdlib.h>
#include <string.h>

#define MAXNAMES 4

/* Global Variables */
/* ----------------------------------------------------------------- */
//1001010
struct namesOfDDS {
	char namelist[MAXNAMES][200];
	int portlist[MAXNAMES];
	int totalnames;
};

//This srtuct holds the whole list of all DDS peers and the total number of them 
static struct namesOfDDS mylist;
//1001010

/* my port */

static char stores_file[256];
static loc_db *loc_stores; /* storage locations */

static char keymap_file[256];
static map_t *keymap; /* keeps meta-data */

static int gossip_period_secs = 0;
static int port; 
static int has_peer = 0;
static char peer_dds[256];
static int peer_port;

static abacus *ab;
static FILE *fd_stats;

/* Abacus events */
enum dds_coord_events {
  EVT_COORD_MSG = 0,
  EVT_COORD_PUT,
  EVT_COORD_GET,
  EVT_COORD_DELETE,
  EVT_COORD_LIST,
  EVT_COORD_GOSSIP_REQ,
  EVT_COORD_GOSSIP_RES,
  EVT_COORD_GETNAME,
  EVT_COORD_GAVENAME,
  EVT_COORD_GOTNAME,
  EVT_COORD_MAX_EVENT /* keep this last */
};

enum dds_coord_tasks {
  TSK_COORD_MSG=0,
  TSK_COORD_PUT,
  TSK_COORD_GET,
  TSK_COORD_DELETE,
  TSK_COORD_LIST,
  TSK_COORD_GOSSIP_REQ,
  TSK_COORD_GOSSIP_RES,
  TSK_COORD_GETNAME,
  TSK_COORD_GAVENAME,
  TSK_COORD_GOTNAME,
  TSK_COORD_MAP_PUT, /* keymap operations */
  TSK_COORD_MAP_GET,
  TSK_COORD_MAP_LIST,
  TSK_COORD_MAP_MERGE,
  TSK_COORD_MAX_TASK /* keep this last */
};

/* Function Headers */
/* ----------------------------------------------------------------- */

/* Hermes Adapters */
static int __dds_validate( hms_endpoint *endpoint, hms_msg *msg );
static int __dds_accepts(  hms_endpoint *endpoint, hms_msg *msg );
static int __dds_handle(   hms_endpoint *endpoint, hms_msg *msg );

/* DDS Helper Functions */
static int __dds_find_verb( char *verb );
static int __dds_dump_stats( FILE *fd );

static int copymylist(struct namesOfDDS *tocopylist);
static int copytomylist(struct namesOfDDS *tocopylist);
static int synclist(struct namesOfDDS *listfrompeer);

/* Gossip */
static int __dds_do_gossip(char *peer_host_name, int peer_port, map_t *keymap);
static int __dds_do_getnames(char *peer_host_name, int peer_port, struct namesOfDDS *mynames);

/* Verb Handlers */
static int __dds_handle_gen( hms_endpoint *endpoint, hms_msg *msg,  int verb_id );
static int __dds_handle_get( hms_endpoint *endpoint, hms_msg *msg,  int verb_id );
static int __dds_handle_put( hms_endpoint *endpoint, hms_msg *msg,  int verb_id );
static int __dds_handle_delete( hms_endpoint *endpoint, hms_msg *msg, int verb_id );
static int __dds_handle_list( hms_endpoint *endpoint, hms_msg *msg, int verb_id );
static int __dds_handle_gossip( hms_endpoint *endpoint, hms_msg *msg, int verb_id );
static int __dds_handle_getnames( hms_endpoint *endpoint, hms_msg *msg, int verb_id );
static int __dds_handle_gavenames( hms_endpoint *endpoint, hms_msg *msg, int verb_id );

/* Main */
/* ----------------------------------------------------------------- */

int main(int argc, char **argv) {

	//initialize mylist
	int i=0;
	for (i=0; i <MAXNAMES; i++){
		bzero(mylist.namelist[i], 200);
		mylist.portlist[i] =0;
	}
	mylist.totalnames =0;
	
	//seed the random number generator
	srand((unsigned)(time(0)));

  int erred = 0, ret = 0;
  hms_ops ops;
  hms *hermes = NULL;
  double last_gossip_time = 0.0;

  /* parse command line arguments */
  if( argc == 7 ) {
    port = atoi( argv[1] );
    strncpy(stores_file, argv[2], 255);
    strncpy(keymap_file, argv[3], 255);
    gossip_period_secs = atoi(argv[4]);
    strncpy(peer_dds, argv[5], 255);
    peer_port = atoi(argv[6]);

    //copy myself and this peer to mylist
    gethostname(mylist.namelist[0], 200);
    mylist.portlist[0] = port;
    mylist.totalnames++;
	//adding other peer
    strcpy(mylist.namelist[1],peer_dds);
    mylist.portlist[1] = peer_port;
    mylist.totalnames++;

    has_peer = 1;					//has_peer gets set to 1 here because we want to connect to a peer now
  } else if( argc == 4 ) {
    port = atoi( argv[1] );
    strncpy(stores_file, argv[2], 255);
    strncpy(keymap_file, argv[3], 255);
    // Lonely, I am so lonely, I must add my self to my own list of names
    gethostname(mylist.namelist[0], 200);
    mylist.portlist[0] = port;
    mylist.totalnames++;
  } else {
    fprintf(stderr, "USAGE: %s <port> <stores_file> <keymap> <gossip-period-secs> <peer-dds-host> <peer-dds-port> (with peering)\n", argv[0] );
    fprintf(stderr, "USAGE: %s <port> <stores_file> <keymap> (without peering)\n", argv[0] );
    fflush(stderr);
    return -1;
  }

  /* start-up abacus */
  {
    char stats_filename[512]; struct utsname hostname;
    ab = abacus_init( TSK_COORD_MAX_TASK, EVT_COORD_MAX_EVENT, 1 );
    uname( &hostname );
    sprintf( stats_filename, "%s_%s_%d.stats", "coord", hostname.nodename, port );
    fd_stats = fopen( stats_filename, "w" );
    if(!fd_stats) { 
      fprintf(stderr, "[ERROR]: Could not open file to dump stats!\n" );
      fflush(stderr);
      return -1; 
    }
  }

  /* load storage nodes */
  dds_locate_init( &loc_stores, stores_file );

  /* initialize keymap */
  keymap = map_init( keymap_file );

  /* setup hermes handlers */
  ops.hms_handle = __dds_handle;
  ops.hms_validate = __dds_validate;
  ops.hms_accepts = __dds_accepts;

  /* start-up hermes */
  hermes = hermes_init(1, port, ops );

  /* do nothing */
  /* hermes runs on its own */
  while(1) { 
    /* HINT: Calling a peer from here would be good! */
    if(has_peer) {
    	//First we get a list of all DDSes on the network
    	__dds_do_getnames(peer_dds, peer_port, &mylist); //sends mylist to the other peer and has it updated

    	//Randomly pick atleast one DDS from this list and connect, disclude myself from this randomization
    	int pickthisfromlist = 1+(int)((mylist.totalnames)*rand()/(RAND_MAX+1.0)); //generates a psuedo-random integer between 0 and mylist.totalnames
    	//Pick the random host from list to gossip with
    	strcpy(peer_dds, mylist.namelist[pickthisfromlist]);
    	peer_port = mylist.portlist[pickthisfromlist];

    	//Gossip business: BEGIN
		double cur_time = abacus_time(ab);
		if( (cur_time - last_gossip_time) > 1000 * gossip_period_secs ) { //timeout for the gossip
			__dds_do_gossip(peer_dds, peer_port, keymap);
			last_gossip_time = cur_time;
		}
		//Gossip business: END
    }
    /* dump stats */
    __dds_dump_stats( fd_stats );
    /* zzzz ! */
    sleep (1);
  }

 exit_main:
  if(hermes) hermes_shutdown( hermes, HMS_TRUE );

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

    unsigned n_events = abacus_event_count(ab, EVT_COORD_MSG, 0);
    unsigned n_puts = abacus_event_count(ab, EVT_COORD_PUT, 0);
    unsigned n_gets = abacus_event_count(ab, EVT_COORD_GET, 0);
    unsigned n_deletes = abacus_event_count(ab, EVT_COORD_DELETE, 0);
    unsigned n_lists = abacus_event_count(ab, EVT_COORD_LIST, 0);
    unsigned n_gossip_reqs = abacus_event_count(ab, EVT_COORD_GOSSIP_REQ, 0);
    unsigned n_gossip_ress = abacus_event_count(ab, EVT_COORD_GOSSIP_RES, 0);

    double   l_events = abacus_task_avgdelay(ab, TSK_COORD_MSG, 0);
    double   l_puts = abacus_task_avgdelay(ab, TSK_COORD_PUT, 0);
    double   l_gets = abacus_task_avgdelay(ab, TSK_COORD_GET, 0);
    double   l_deletes = abacus_task_avgdelay(ab, TSK_COORD_DELETE, 0);
    double   l_lists = abacus_task_avgdelay(ab, TSK_COORD_LIST, 0);
    double   l_gossip_reqs = abacus_task_avgdelay(ab, TSK_COORD_GOSSIP_REQ, 0);
    double   l_gossip_ress = abacus_task_avgdelay(ab, TSK_COORD_GOSSIP_RES, 0);

    fprintf(fd, "TIME: %10.3lf N_EVTS: %5u N_PUTS: %5u N_GETS: %5u N_DELETES: %5u N_LISTS: %5u N_GOSSIP_REQS: %5u N_GOSSIP_RESS: %5u  L_EVTS: %7.3lf L_PUTS: %7.3lf L_GETS: %7.3lf L_DELETES: %7.3lf L_LISTS: %7.3lf L_GOSSIP_REQS: %7.3lf L_GOSSIP_RESS: %7.3lf\n",
	    abacus_time(ab),
	    n_events, n_puts, n_gets, n_deletes, n_lists, n_gossip_reqs, n_gossip_ress,
	    l_events, l_puts, l_gets, l_deletes, l_lists, l_gossip_reqs, l_gossip_ress
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

  char *verb = NULL;
  int will_accept;
  
  /* get verb */
  hms_msg_get_verb( msg, &verb );

  if( __dds_find_verb( verb ) >= 0 && __dds_find_verb( verb ) < DDS_MAX ) {
    will_accept = 0;
  } else { will_accept = -1; }
  
  /* free verb */
  if(verb) free(verb); verb = NULL;

  return will_accept;

} 


static int __dds_handle(  hms_endpoint *endpoint, hms_msg *msg ) {

  int ret = 0, erred = 0;
  char *verb = NULL;

  guid_t *id = NULL;

  /* Record event in abacus */
  ret = abacus_event_add( ab, EVT_COORD_MSG, 0 );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not log event into abacus", cleanup, &erred );

  /* Record event in abacus */
  id = guid_create();
  DIE_IF_EQUAL( (int) id, (int) NULL, "Could not create guid", cleanup, &erred );
  ret = abacus_task_add( ab, id );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not add task into abacus", cleanup, &erred );
  ret = abacus_task_start( ab, id, TSK_COORD_MSG, 0);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not add task into abacus", cleanup, &erred );

  hms_msg_get_verb( msg, &verb );
  fprintf(stdout, "[NOTE]: Handling [%s]...\n", verb );

  /* call different verb handler */
  int vid = __dds_find_verb( verb );
  switch( vid ) {
  case DDS_PUT:
    ret = __dds_handle_put( endpoint, msg, vid );
    break;
  case DDS_GET:
    ret = __dds_handle_get( endpoint, msg, vid );
    break;
  case DDS_DELETE:
    ret = __dds_handle_delete( endpoint, msg, vid );
    break;
  case DDS_LISTBUCKET:
    ret = __dds_handle_list( endpoint, msg, vid );
    break;
  case DDS_GOSSIP:
    ret = __dds_handle_gossip( endpoint, msg, vid );
    break;
  case DDS_GETNAMES:
  	ret = __dds_handle_getnames(endpoint, msg, vid);
  	break;
  case DDS_GAVENAMES:
	ret = __dds_handle_gavenames(endpoint, msg, vid);
	break;
  default:
    fprintf(stdout, "No verb handler for [%s]\n", verb );
    break;
  }

  /* Delete task from abacus */
  ret = abacus_task_end(ab, id, TSK_COORD_MSG, 0);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not delete task abacus", cleanup, &erred );
  ret = abacus_task_delete(ab, id);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not delete task abacus", cleanup, &erred );
  guid_destroy( id ); id = NULL;

  /* free memory */
  if(verb) { free(verb); verb = NULL; }

 cleanup:
  if(verb) { free(verb); verb = NULL; }
  return ret; /* everything ok */

}

//1001010
/* Get names of all DDS */
/* ----------------------------------------------------------------- */

static int __dds_do_getnames(char *peer_host_name, int peer_port, struct namesOfDDS *mynames) {

  int ret = 0, erred = 0;
  
  char *buffer = NULL;
  
  hms_ops ops;
  hms_endpoint *conn = NULL;
  hms_msg *msg = NULL;
  guid_t *id = NULL;

 /* Record event in abacus */
  ret = abacus_event_add( ab, EVT_COORD_GETNAME, 0 );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not log event into abacus", err, &erred );

  /* Start task */
  id = guid_create();
  DIE_IF_EQUAL( (int) id, (int) NULL, "Could not create guid", err, &erred );
  ret = abacus_task_add( ab, id );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not add task into abacus", err, &erred );
  ret = abacus_task_start( ab, id, TSK_COORD_GETNAME, 0);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not start task in abacus", err, &erred );
  
  // Put into buffer
  buffer = ( char * ) malloc(sizeof(struct namesOfDDS));
  DIE_IF_EQUAL( (int) buffer, (int) NULL, "Could not make buffer", err, &erred );
  memcpy( buffer, mynames, sizeof(struct namesOfDDS));
  
  /* Connect to peer */
  fprintf(stdout, "Exchanging name list with peer at %s:%d\n", peer_host_name, peer_port );
  int conn_fd = hms_endpoint_connect( peer_host_name, peer_port );
  DIE_IF_EQUAL( conn_fd, -1, "Could not connect to dds peer", err, &erred );
  /* Init Endpoint */
  conn = hms_endpoint_init( conn_fd, ops );
  DIE_IF_EQUAL( conn, NULL,"Could not initialize endpoint", err, &erred );
  /* Send a message to peer */
  msg = hms_msg_create();
  DIE_IF_EQUAL( (int) msg, (int) NULL, "Could not create msg", err, &erred);
  ret = hms_msg_set_verb( msg, dds_verbs[DDS_GETNAMES]);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not set header", err, &erred);
  ret = hms_msg_set_body( msg, buffer, sizeof(struct namesOfDDS) );
  DIE_IF_NOT_EQUAL(  ret, 0, "Could not add msg body", err, &erred );
  ret = hms_endpoint_send_msg( conn, msg );
  DIE_IF_NOT_EQUAL(  ret, 0, "Could not forward msg to peer", err, &erred );
  /* Cleanup */
  ret = hms_msg_destroy( msg ); msg = NULL;
  DIE_IF_NOT_EQUAL(  ret, 0, "Could not destroy msg", err, &erred );
  ret = hms_endpoint_destroy( conn ); conn = NULL;
  DIE_IF_NOT_EQUAL(  ret, 0, "Could not destroy endpoint", err, &erred );

 err:
  
  /* Record into abacus */
  ret = abacus_task_end(ab, id, TSK_COORD_GETNAME, 0);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not delete task abacus", cleanup, &erred );
  ret = abacus_task_delete(ab, id);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not delete task abacus", cleanup, &erred );
  guid_destroy( id ); id = NULL;

 cleanup:
  if(msg) { hms_msg_destroy(msg); msg = NULL; }
  if(buffer) { free(buffer); buffer = NULL; }
  if(conn) { free(conn); conn = NULL; }
  if(id) { guid_destroy(id); id = NULL; }

  return erred;

}
//1001010

/* Gossip */
/* ----------------------------------------------------------------- */

static int __dds_do_gossip(char *peer_host_name, int peer_port, map_t *map) {

  int ret = 0, erred = 0;
  inode_t *nodes = NULL; unsigned n_nodes;
  
  char *buffer = NULL;
  
  hms_ops ops;
  hms_endpoint *conn = NULL;
  hms_msg *msg = NULL;
  guid_t *id = NULL;

 /* Record event in abacus */
  ret = abacus_event_add( ab, EVT_COORD_GOSSIP_REQ, 0 );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not log event into abacus", err, &erred );

  /* Start task */
  id = guid_create();
  DIE_IF_EQUAL( (int) id, (int) NULL, "Could not create guid", err, &erred );
  ret = abacus_task_add( ab, id );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not add task into abacus", err, &erred );
  ret = abacus_task_start( ab, id, TSK_COORD_GOSSIP_REQ, 0);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not start task in abacus", err, &erred );

  /* Get a list of all objects */
  ret = map_listall(map, &nodes, &n_nodes );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not get object list from Keymap", err, &erred );

  /* Put into buffer */
  buffer = ( char * ) malloc( n_nodes * sizeof(inode_t) );
  DIE_IF_EQUAL( (int) buffer, (int) NULL, "Could not make buffer", err, &erred );
  memcpy( buffer, nodes, n_nodes * sizeof(inode_t) );

  /* Do I have anything to send? */
  if(n_nodes == 0 || nodes == NULL ) { goto err; }

  /* Connect to peer */
  fprintf(stdout, "Gossiping with peer at %s:%d\n", peer_host_name, peer_port );
  int conn_fd = hms_endpoint_connect( peer_host_name, peer_port );
  DIE_IF_EQUAL( conn_fd, -1, "Could not connect to dds peer", err, &erred );
  /* Init Endpoint */
  conn = hms_endpoint_init( conn_fd, ops );
  DIE_IF_EQUAL( conn, NULL,"Could not initialize endpoint", err, &erred );
  /* Send a message to peer */
  msg = hms_msg_create();
  DIE_IF_EQUAL( (int) msg, (int) NULL, "Could not create msg", err, &erred);
  ret = hms_msg_set_verb( msg, dds_verbs[DDS_GOSSIP] );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not set header", err, &erred);
  ret = hms_msg_set_body( msg, buffer, n_nodes * sizeof(inode_t) );
  DIE_IF_NOT_EQUAL(  ret, 0, "Could not add msg body", err, &erred );
  ret = hms_endpoint_send_msg( conn, msg );
  DIE_IF_NOT_EQUAL(  ret, 0, "Could not forward msg to peer", err, &erred );
  /* Cleanup */
  ret = hms_msg_destroy( msg ); msg = NULL;
  DIE_IF_NOT_EQUAL(  ret, 0, "Could not destroy msg", err, &erred );
  ret = hms_endpoint_destroy( conn ); conn = NULL;
  DIE_IF_NOT_EQUAL(  ret, 0, "Could not destroy endpoint", err, &erred );

 err:
  
  /* Record into abacus */
  ret = abacus_task_end(ab, id, TSK_COORD_GOSSIP_REQ, 0);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not delete task abacus", cleanup, &erred );
  ret = abacus_task_delete(ab, id);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not delete task abacus", cleanup, &erred );
  guid_destroy( id ); id = NULL;

 cleanup:
  if(nodes) { free(nodes); nodes = NULL; }
  if(msg) { hms_msg_destroy(msg); msg = NULL; }
  if(buffer) { free(buffer); buffer = NULL; }
  if(conn) { free(conn); conn = NULL; }
  if(id) { guid_destroy(id); id = NULL; }

  return erred;

}

/* Verb Handlers */
/* ----------------------------------------------------------------- */

static int __dds_handle_put( hms_endpoint *endpoint, hms_msg *msg, int verb_id ) {

  int ret = 0, erred = 0;
  char *bucket_name = NULL, *key_name = NULL, filename[512];
  char *store_ack = NULL;
  hms_endpoint *conn = NULL;
  hms_msg *reply = NULL;
  char status_code[9];

  loc_t *store_locations = NULL;
  int n_store_locations = 0;
  guid_t *id = NULL;

  /* Record event in abacus */
  ret = abacus_event_add( ab, EVT_COORD_PUT, 0 );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not log event into abacus", err, &erred );

  /* Start task */
  id = guid_create();
  DIE_IF_EQUAL( (int) id, (int) NULL, "Could not create guid", err, &erred );
  ret = abacus_task_add( ab, id );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not add task into abacus", err, &erred );
  ret = abacus_task_start( ab, id, TSK_COORD_PUT, 0);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not start task in abacus", err, &erred );

  /* Extract Bucket and File */
  hms_msg_get_named_header( msg , "Bucket", &bucket_name );
  hms_msg_get_named_header( msg,  "Key", &key_name );

  /* Check if the values are correct */
  DIE_IF_EQUAL( (int) bucket_name, (int) NULL, "Bucket is not defined", err, &erred );
  DIE_IF_EQUAL( (int) key_name, (int) NULL, "Key is not defined", err, &erred );

  /* Compose object */
  obj_t object;
  memset( &object, 0, sizeof(obj_t) );
  strncpy(object.bucket_name, bucket_name, MAX_BUCKET_NAME_LEN - 1);
  strncpy(object.key_name, key_name, MAX_KEY_NAME_LEN - 1);

  /* Find out storage node to contact */
  ret = dds_locate_storage_node( loc_stores, verb_id, &object, &store_locations, &n_store_locations);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not find any storage locations", err, &erred );

  /* Connect to storage node and forward request */
  /* If multiple storage nodes exist, then replicate to a subset of them */
  hms_ops ops;
  int i = 0;
  for(i=0; i < n_store_locations; i++) {
    /* Inform */
    fprintf(stdout, "[NOTE]: PUT %s:%s at %s:%d\n",
    bucket_name, key_name, 
    store_locations[i].host_name, store_locations[i].port );
    /* Open Connection */
    int conn_fd = hms_endpoint_connect( store_locations[i].host_name,store_locations[i].port );
    DIE_IF_EQUAL( conn_fd, -1, "Could not connect to storage node", err, &erred );
    /* Init Endpoint */
    conn = hms_endpoint_init( conn_fd, ops );
    DIE_IF_EQUAL( conn, NULL,"Could not initialize endpoint", err, &erred );
    /* Send Request */
    ret = hms_endpoint_send_msg( conn, msg);
    DIE_IF_EQUAL(ret , -1, "Could not forward request", err, &erred );
    /* Get Reply */
    reply = NULL;
    ret =  hms_endpoint_recv_msg( conn, &reply );
    DIE_IF_EQUAL( ret, -1, "Didn't get reply from storage node", err, &erred );
    /* Check if store said OK */
    ret = hms_msg_get_verb( reply, &store_ack );
    DIE_IF_NOT_EQUAL( ret, 0, "Could not figure out what store said", err, &erred );
    ret = strncmp( store_ack, "OK", 2);
    DIE_IF_NOT_EQUAL( ret, 0, "Store did not say OK", err, &erred );
    /* Cleanup */
    ret = hms_msg_destroy( reply ); reply = NULL;
    DIE_IF_NOT_EQUAL( ret, 0, "Could not destroy reply message", err, &erred );
    ret = hms_endpoint_destroy( conn );  conn = NULL;
    DIE_IF_NOT_EQUAL( ret, 0, "Could not destroy connection", err, &erred );
  }

  /* Record version */
  if( verb_id == DDS_PUT ) {
    /* HINT: Record entry into keymap */
    struct timeval tv; gettimeofday( &tv, NULL );
    ret = map_put(keymap, &object, store_locations, n_store_locations, tv.tv_sec);
    DIE_IF_NOT_EQUAL( ret, 0, "Could not add meta-data into KeyMap", err, &erred );
    fprintf(stdout, "[NOTE]: PUT of %s:%s was successful\n", bucket_name, key_name );
  }

  /* Send Reply */
  reply = hms_msg_create();
  DIE_IF_EQUAL( (int) reply, (int) NULL, "Could not create reply", err, &erred);
  ret = hms_msg_set_verb( reply, "OK" );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not set header", err, &erred);
  ret = hms_msg_add_named_header( reply, "Bucket", bucket_name );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not add Bucket named header", err, &erred);
  ret = hms_msg_add_named_header( reply, "Key", key_name );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not add Key named header", err, &erred);
  ret = hms_endpoint_send_msg( endpoint, reply );
  DIE_IF_NOT_EQUAL(  ret, 0, "Could not forward reply to user", err, &erred );
  ret = hms_msg_destroy( reply ); reply = NULL;
  DIE_IF_NOT_EQUAL(  ret, 0, "Could not destroy reply", err, &erred );

 err:

  /* Record end into abacus */
  ret = abacus_task_end(ab, id, TSK_COORD_PUT, 0);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not delete task abacus", cleanup, &erred );
  ret = abacus_task_delete(ab, id);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not delete task abacus", cleanup, &erred );
  guid_destroy( id ); id = NULL;

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
  if( bucket_name ) { free(bucket_name); bucket_name = NULL; }
  if( key_name ) { free(key_name); key_name = NULL; }
  if( reply ) { hms_msg_destroy( reply ); reply = NULL; }
  if( store_locations ) { free(store_locations); store_locations = NULL; }
  if( conn ) { hms_endpoint_destroy(conn); conn = NULL; }
  if( id ) { guid_destroy(id); id = NULL; }
  return erred;

}

static int __dds_handle_get( hms_endpoint *endpoint, hms_msg *msg, int verb_id ) {

  int erred = 0;
  char *bucket_name = NULL, *key_name = NULL, filename[512];
  
  int ret = 0;
  hms_endpoint *conn = NULL;
  hms_msg *reply = NULL;
  char status_code[9];

  loc_t *store_nodes = NULL;   int n_store_nodes;
  int is_deleted = 0;

  guid_t *id = NULL;

  /* Record event in abacus */
  ret = abacus_event_add( ab, EVT_COORD_GET, 0 );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not log event into abacus", cleanup, &erred );

  /* Start task */
  id = guid_create();
  DIE_IF_EQUAL( (int) id, (int) NULL, "Could not create guid", err, &erred );
  ret = abacus_task_add( ab, id );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not add task into abacus", err, &erred );
  ret = abacus_task_start( ab, id, TSK_COORD_GET, 0);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not start task in abacus", err, &erred );

  /* Extract Bucket and File */
  hms_msg_get_named_header( msg , "Bucket", &bucket_name );
  hms_msg_get_named_header( msg,  "Key", &key_name );

  /* Check if the values are correct */
  DIE_IF_EQUAL( (int) bucket_name, (int) NULL, "Bucket is not defined", err, &erred );
  DIE_IF_EQUAL( (int) key_name, (int) NULL, "Key is not defined", err, &erred );

  /* Find out storage node to contact */
  obj_t object;
  memset( &object, 0, sizeof(obj_t) );
  strncpy(object.bucket_name, bucket_name, MAX_BUCKET_NAME_LEN - 1);
  strncpy(object.key_name, key_name, MAX_KEY_NAME_LEN - 1);

  /* Check Keymap to locate the object */
  ret = map_get(keymap,&object,&store_nodes,&n_store_nodes,&is_deleted);
  DIE_IF_NOT_EQUAL(ret,0,"Could not find object", err, &erred );

  /* Check if object is already deleted */
  DIE_IF_NOT_EQUAL(is_deleted, 0, "Object is already deleted", err, &erred );

  /* If not deleted, go and fetch the object */
  /* Connect to storage node and forward request */
  /* TODO: Add retry logic to handle failures */
  hms_ops ops;
  int s_node = random() % n_store_nodes;
  
  /* Open Connection */
  int conn_fd = hms_endpoint_connect( store_nodes[s_node].host_name,store_nodes[s_node].port );
  DIE_IF_EQUAL( conn_fd, -1, "Could not connect to storage node",err, &erred );
  /* Init Endpoint */
  conn = hms_endpoint_init( conn_fd, ops );
  DIE_IF_EQUAL( (int) conn, (int) NULL,"Could not initialize endpoint",err, &erred );
  /* Send Request */
  ret = hms_endpoint_send_msg( conn, msg);
  DIE_IF_EQUAL(ret , -1, "Could not forward request",err, &erred );
  /* Get Reply */
  reply = NULL;
  ret =  hms_endpoint_recv_msg( conn, &reply );
  DIE_IF_EQUAL( ret, -1, "Didn't get reply from storage node",err, &erred );  
  /* Forward Reply */
  ret = hms_endpoint_send_msg( endpoint, reply );
  DIE_IF_EQUAL(  ret, -1, "Could not forward reply to user",err, &erred );
  /* Delete message */
  ret = hms_msg_destroy( reply ); reply = NULL;
  DIE_IF_NOT_EQUAL( ret, 0, "Could not send delete reply",err, &erred );
  /* Close Connection */
  ret = hms_endpoint_destroy( conn ); conn = NULL;
  DIE_IF_NOT_EQUAL( ret, 0, "Could not close connection to storage node",err, &erred );

 err:

  /* Record end into abacus */
  ret = abacus_task_end(ab, id, TSK_COORD_GET, 0);
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

 cleanup:  
  /* free before leaving */
  if( bucket_name ) { free(bucket_name); bucket_name = NULL; }
  if( key_name ) { free(key_name); key_name = NULL; }
  if( reply ) { hms_msg_destroy( reply ); reply = NULL; }
  if( store_nodes ) { free(store_nodes); store_nodes = NULL; }
  if( conn ) { hms_endpoint_destroy( conn ); conn = NULL; }
  if( id ) { guid_destroy( id ); id = NULL; }
  return erred;

}

static int __dds_handle_delete( hms_endpoint *endpoint, hms_msg *msg, int verb_id ) {

  int erred = 0;
  char *bucket_name = NULL, *key_name = NULL, filename[512];
  
  int ret = 0;
  hms_endpoint *conn = NULL;
  hms_msg *reply = NULL;
  char status_code[9];

  loc_t *store_nodes = NULL;   int n_store_nodes = 0;
  int is_deleted = 0;

  guid_t *id = NULL;

  /* Record event in abacus */
  ret = abacus_event_add( ab, EVT_COORD_DELETE, 0 );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not log event into abacus", cleanup, &erred );

  /* Start task */
  id = guid_create();
  DIE_IF_EQUAL( (int) id, (int) NULL, "Could not create guid", err, &erred );
  ret = abacus_task_add( ab, id );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not add task into abacus", err, &erred );
  ret = abacus_task_start( ab, id, TSK_COORD_DELETE, 0);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not start task in abacus", err, &erred );

  /* Extract Bucket and File */
  hms_msg_get_named_header( msg , "Bucket", &bucket_name );
  hms_msg_get_named_header( msg,  "Key", &key_name );

  /* Check if the values are correct */
  DIE_IF_EQUAL( (int) bucket_name, (int) NULL, "Bucket is not defined", err, &erred );
  DIE_IF_EQUAL( (int) key_name, (int) NULL, "Key is not defined", err, &erred );

  /* Find out storage node to contact */
  obj_t object;
  memset( &object, 0, sizeof(obj_t) );
  strncpy(object.bucket_name, bucket_name, MAX_BUCKET_NAME_LEN - 1);
  strncpy(object.key_name, key_name, MAX_KEY_NAME_LEN - 1);

  /* Check Keymap to locate the object */
  ret = map_get(keymap,&object,&store_nodes,&n_store_nodes,&is_deleted);
  DIE_IF_NOT_EQUAL(ret,0,"Could not find object", err, &erred );

  /* Delete key */
  if( !is_deleted ) {
    /* HINT: Delete entry from keymap */
    struct timeval tv; gettimeofday( &tv, NULL );
    ret = map_del(keymap, &object, tv.tv_sec );
    DIE_IF_NOT_EQUAL( ret, 0, "Could not delete object from Keymap", err, &erred );
  }

  /* Connect to storage node and forward request */
  /* If multiple storage nodes exist, then delete from all of them */
  hms_ops ops;
  int i = 0;
  for(i=0; i < n_store_nodes; i++) {
    /* Inform */
    fprintf(stdout, "[NOTE]: DELETE %s:%s at %s:%d\n",
    bucket_name, key_name, 
    store_nodes[i].host_name, store_nodes[i].port );
    /* Open Connection */
    int conn_fd = hms_endpoint_connect( store_nodes[i].host_name,store_nodes[i].port );
    DIE_IF_EQUAL( conn_fd, -1, "Could not connect to storage node", err, &erred );
    /* Init Endpoint */
    conn = hms_endpoint_init( conn_fd, ops );
    DIE_IF_EQUAL( (int) conn, (int) NULL,"Could not initialize endpoint", err, &erred );
    /* Send Request */
    ret = hms_endpoint_send_msg( conn, msg);
    DIE_IF_EQUAL(ret , -1, "Could not forward request", err, &erred );
    /* Get Reply */
    reply = NULL;
    ret =  hms_endpoint_recv_msg( conn, &reply );
    DIE_IF_NOT_EQUAL( ret, 0, "Didn't get reply from storage node", err, &erred );
    /* Cleanup */
    ret = hms_msg_destroy( reply ); reply = NULL;
    DIE_IF_NOT_EQUAL( ret, 0, "Could not destroy reply message", err, &erred );
    ret = hms_endpoint_destroy( conn ); conn = NULL;
    DIE_IF_NOT_EQUAL( ret, 0, "Could not destroy endpoint", err, &erred );
  }


  /* Send Reply */
  reply = hms_msg_create();
  DIE_IF_EQUAL( (int) reply, (int) NULL, "Could not create reply", err, &erred);
  ret = hms_msg_set_verb( reply, "OK" );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not set header", err, &erred);
  if( bucket_name && key_name ) {
    ret = hms_msg_add_named_header( reply, "Bucket", bucket_name );
    DIE_IF_NOT_EQUAL( ret, 0, "Could not add Bucket named header", err, &erred);
    ret = hms_msg_add_named_header( reply, "Key", key_name );
    DIE_IF_NOT_EQUAL( ret, 0, "Could not add Key named header", err, &erred);
  }
  ret = hms_endpoint_send_msg( endpoint, reply );
  DIE_IF_NOT_EQUAL(  ret, 0, "Could not forward reply to user", err, &erred );
  ret = hms_msg_destroy( reply ); reply = NULL;
  DIE_IF_NOT_EQUAL(  ret, 0, "Could not destroy reply", err, &erred );

 err:

  /* Record end into abacus */
  ret = abacus_task_end(ab, id, TSK_COORD_DELETE, 0);
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

 cleanup:  
  /* free before leaving */
  if( bucket_name ) { free(bucket_name); }
  if( key_name ) { free(key_name); }
  if( reply ) hms_msg_destroy( reply );
  if( store_nodes ) { free(store_nodes); store_nodes = NULL; }
  if( id ) { guid_destroy( id ); id = NULL; }

  return erred;

}

static int __dds_handle_list( hms_endpoint *endpoint, hms_msg *msg, int verb_id ) {

  int i = 0;
  int ret = 0, erred = 0;
  char *bucket_name = NULL, *key_name = NULL;
  char dir_name[512];

  int n;
  hms_msg *reply = NULL;

  inode_t *inodes = NULL;
  unsigned n_inodes;
  int show_deleted = 0;
  char *buffer = NULL;

  guid_t *id = NULL;

  /* Record event in abacus */
  ret = abacus_event_add( ab, EVT_COORD_LIST, 0 );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not log event into abacus", cleanup, &erred );

  /* Start task */
  id = guid_create();
  DIE_IF_EQUAL( (int) id, (int) NULL, "Could not create guid", err, &erred );
  ret = abacus_task_add( ab, id );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not add task into abacus", err, &erred );
  ret = abacus_task_start( ab, id, TSK_COORD_LIST, 0);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not start task in abacus", err, &erred );

  /* Extract Bucket and File */
  ret = hms_msg_get_named_header( msg , "Bucket", &bucket_name );
  DIE_IF_NOT_EQUAL(ret, 0, "Bucket is not defined", err, &erred );

  /* Key is optional */
  ret = hms_msg_get_named_header( msg,  "Key", &key_name );

  /* Check if the values are correct */
  DIE_IF_EQUAL( (int) bucket_name, (int) NULL, "Bucket is not defined", err, &erred );

  /* Find out storage node to contact */
  obj_t object; memset( &object, 0, sizeof(obj_t) );
  strncpy(object.bucket_name, bucket_name, MAX_BUCKET_NAME_LEN - 1);

  /* Key may not be given */
  if(key_name) { strncpy(object.key_name, key_name, MAX_KEY_NAME_LEN - 1); }
  else { memset(object.key_name, '\0', MAX_KEY_NAME_LEN - 1); }

  /* List the objects that matched */
  ret = map_list( keymap, &object, &inodes, &n_inodes, show_deleted );
  DIE_IF_NOT_EQUAL( ret, 0, "Listing map failed", err, &erred );

  /* Nothing in the list :( */
  if( ret == 0 && n_inodes > 0 && inodes != NULL ) {
    /* Malloc space for list */
    buffer = (char *) malloc( n_inodes * (MAX_KEY_NAME_LEN + 2) );
    DIE_IF_EQUAL( (int) buffer, (int) NULL, "Could not malloc space for buffer", err, &erred );
    
    /* Copy list into buffer */
    char *p = buffer;
    for(i=0; i < n_inodes; i++) {
      sprintf( p, "%s\n", inodes[i].object.key_name );
      p += strlen(inodes[i].object.key_name) + 1;
    }
  }

  /* Send Reply */
  reply = hms_msg_create();
  DIE_IF_EQUAL( (int) reply, (int) NULL, "Could not create reply", err, &erred);
  if( !erred ) { ret = hms_msg_set_verb( reply, "OK" ); }
  else { ret = hms_msg_set_verb( reply, "ERROR" ); }
  DIE_IF_NOT_EQUAL( ret, 0, "Could not set header", err, &erred);
  ret = hms_msg_add_named_header( reply, "Bucket", bucket_name );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not add Bucket named header", err, &erred);

  if( buffer ) { /* no list to send */
    ret = hms_msg_set_body( reply, buffer, strlen(buffer) );
    DIE_IF_NOT_EQUAL( ret, 0, "Could not add message body", err, &erred);
  }

  ret = hms_endpoint_send_msg( endpoint, reply );
  DIE_IF_NOT_EQUAL(  ret, 0, "Could not forward reply to user", err, &erred );
  ret = hms_msg_destroy( reply ); reply = NULL;
  DIE_IF_NOT_EQUAL(  ret, 0, "Could not destroy reply", err, &erred );

 err:

  /* Record end into abacus */
  ret = abacus_task_end(ab, id, TSK_COORD_LIST, 0);
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
    }
    if( key_name ) {
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
  if(bucket_name) { free(bucket_name); bucket_name = NULL; }
  if(key_name) { free(key_name); key_name = NULL; }
  if(reply) { hms_msg_destroy(reply); reply=NULL; }
  if(inodes) { free(inodes); inodes = NULL; }
  if(buffer) { free(buffer); buffer = NULL; }
  if(id) { guid_destroy(id); id = NULL; }

  return erred;

} /* end __dds_handle_list() */

static int __dds_handle_gossip( hms_endpoint *endpoint, hms_msg *msg, int verb_id ) {

  unsigned i = 0;
  int ret = 0, erred = 0;

  inode_t *inodes = NULL;
  unsigned n_inodes;
  char *buffer = NULL; int buffer_len = 0;

  guid_t *id = NULL;

  /* Record event in abacus */
  ret = abacus_event_add( ab, EVT_COORD_GOSSIP_RES, 0 );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not log event into abacus", err, &erred );

  /* Start task */
  id = guid_create();
  DIE_IF_EQUAL( (int) id, (int) NULL, "Could not create guid", err, &erred );
  ret = abacus_task_add( ab, id );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not add task into abacus", err, &erred );
  ret = abacus_task_start( ab, id, TSK_COORD_GOSSIP_RES, 0);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not start task in abacus", err, &erred );

  /* Find how many entries */
  buffer_len = hms_msg_get_body_size(msg);
  ret = hms_msg_get_body( msg, &buffer, &buffer_len );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not get body", err, &erred );
  n_inodes = buffer_len / sizeof(inode_t);

  /* Nothing to merge */
  if( n_inodes == 0 || buffer == NULL ) { goto err; }
  else {
    fprintf(stdout, "[NOTE] Merging %u entries from peer DDS\n", n_inodes );
    fflush(stdout);
  }


  /* Cast to inodes */
  inodes = (inode_t *) buffer;
  ret = map_merge( keymap, inodes, n_inodes );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not merge data obtained from gossip", err, &erred );

 err:

  /* Record into abacus */
  ret = abacus_task_end(ab, id, TSK_COORD_GOSSIP_RES, 0);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not delete task abacus", cleanup, &erred );
  ret = abacus_task_delete(ab, id);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not delete task abacus", cleanup, &erred );
  guid_destroy( id ); id = NULL;
  
 cleanup:
  if(buffer) { free(buffer); buffer = NULL; }
  if(id) { guid_destroy(id); id = NULL; }

  return erred;

}

//1001010
//Handle the request for list of all DDS names
static int __dds_handle_getnames( hms_endpoint *endpoint, hms_msg *msg, int verb_id ) {

  unsigned i = 0;
  int ret = 0, erred = 0;
  struct namesOfDDS *listtosend;
  char *buffer = NULL; int buffer_len = 0;

  hms_msg *reply = NULL;
  guid_t *id = NULL;

  /* Record event in abacus */
  ret = abacus_event_add( ab, EVT_COORD_GAVENAME, 0 );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not log event into abacus", err, &erred );

  /* Start task */
  id = guid_create();
  DIE_IF_EQUAL( (int) id, (int) NULL, "Could not create guid", err, &erred );
  ret = abacus_task_add( ab, id );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not add task into abacus", err, &erred );
  ret = abacus_task_start( ab, id, TSK_COORD_GAVENAME, 0);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not start task in abacus", err, &erred );

  //read buffer from packet
  buffer_len = hms_msg_get_body_size(msg);
  ret = hms_msg_get_body( msg, &buffer, &buffer_len );
  DIE_IF_NOT_EQUAL( ret, 0, "Could not get body", err, &erred );

  //make a copy of the list
 
  listtosend = (struct namesOfDDS*) buffer;
  //synchronize my list to this peers list (add the peers name to my list if not already there)
  ret = synclist(listtosend);
  //now i make a copy of my newly updated list and send it back to peer
  ret = copymylist(listtosend);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not create a copy list", err, &erred );

  buffer = ( char * ) malloc(sizeof(struct namesOfDDS));
  DIE_IF_EQUAL( (int) buffer, (int) NULL, "Could not make buffer", err, &erred );
  memcpy( buffer, listtosend, sizeof(struct namesOfDDS));

  //send this new list to the requester
	reply = hms_msg_create();
    DIE_IF_EQUAL( (int) reply, (int) NULL, "Could not create reply", err, &erred);
    ret = hms_msg_set_verb( reply, dds_verbs[DDS_GAVENAMES]);
    DIE_IF_NOT_EQUAL( ret, 0, "Could not set header", err, &erred);

    if( buffer ) { /* no list to send */
      ret = hms_msg_set_body( reply, buffer, strlen(buffer) );
      DIE_IF_NOT_EQUAL( ret, 0, "Could not add message body", err, &erred);
    }

    ret = hms_endpoint_send_msg( endpoint, reply );
    DIE_IF_NOT_EQUAL(  ret, 0, "Could not forward reply to user", err, &erred );
    ret = hms_msg_destroy( reply ); reply = NULL;
    DIE_IF_NOT_EQUAL(  ret, 0, "Could not destroy reply", err, &erred );
  //
 err:

  /* Record into abacus */
  ret = abacus_task_end(ab, id, TSK_COORD_GAVENAME, 0);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not delete task abacus", cleanup, &erred );
  ret = abacus_task_delete(ab, id);
  DIE_IF_NOT_EQUAL( ret, 0, "Could not delete task abacus", cleanup, &erred );
  guid_destroy( id ); id = NULL;
  
 cleanup:
  if(buffer) { free(buffer); buffer = NULL; }
  if(id) { guid_destroy(id); id = NULL; }

  return erred;

}

static int __dds_handle_gavenames( hms_endpoint *endpoint, hms_msg *msg, int verb_id ) {
	unsigned i = 0;
	int ret = 0, erred = 0;
	struct namesOfDDS *listtocopy;
	char *buffer = NULL; int buffer_len = 0;

	hms_msg *reply = NULL;
	guid_t *id = NULL;

	/* Record event in abacus */
	ret = abacus_event_add( ab, EVT_COORD_GOTNAME, 0 );
	DIE_IF_NOT_EQUAL( ret, 0, "Could not log event into abacus", err, &erred );

	/* Start task */
	id = guid_create();
	DIE_IF_EQUAL( (int) id, (int) NULL, "Could not create guid", err, &erred );
	ret = abacus_task_add( ab, id );
	DIE_IF_NOT_EQUAL( ret, 0, "Could not add task into abacus", err, &erred );
	ret = abacus_task_start( ab, id, TSK_COORD_GOTNAME, 0);
	DIE_IF_NOT_EQUAL( ret, 0, "Could not start task in abacus", err, &erred );

	//read buffer from packet
	buffer_len = hms_msg_get_body_size(msg);
	ret = hms_msg_get_body( msg, &buffer, &buffer_len );
	DIE_IF_NOT_EQUAL( ret, 0, "Could not get body", err, &erred );

	//make a copy of the list
	listtocopy = (struct namesOfDDS*) buffer;
	ret = copytomylist(listtocopy);
	DIE_IF_NOT_EQUAL( ret, 0, "Could not create a copy list", err, &erred );

	err:
		//record it in abacus
		ret = abacus_task_end(ab, id, TSK_COORD_GOTNAME, 0);
		DIE_IF_NOT_EQUAL( ret, 0, "Could not delete task abacus", cleanup, &erred );
		ret = abacus_task_delete(ab, id);
		DIE_IF_NOT_EQUAL( ret, 0, "Could not delete task abacus", cleanup, &erred );
		guid_destroy( id ); id = NULL;

	cleanup:
		if(buffer) { free(buffer); buffer = NULL; }
		if(id) { guid_destroy(id); id = NULL; }

		return erred;
}

//copy from my DDSlist to the one passed in this function
static int copymylist(struct namesOfDDS *tocopylist) {
	int i =0;
	for	(i =0; i < MAXNAMES; i++){
		strcpy(tocopylist->namelist[i], mylist.namelist[i]);
		tocopylist->portlist[i] = mylist.portlist[i];
	}
	tocopylist->totalnames = mylist.totalnames;
	return 0;
}

//copy from the given DDS list to mylist
static int copytomylist(struct namesOfDDS *tocopylist) {
	int i =1, j=0;
	int tocopyflags[4] = {1,1,1,1};
	
	//mark the tocopyflags as required to prepare to for copying
	for (i =0; i<MAXNAMES; i++){//check if the peers name is alredy in the list or not
		for(j=0; j<MAXNAMES; j++) {
			if(strcmp(mylist.namelist[i], tocopylist->namelist[j]) == 0){
				tocopyflags[j] = 0;	
			}
		}
	}
	
	//find the first NULL element in mylist and add the the elements i marked in tocopyflags[]
	for	(i =1; i < MAXNAMES; i++){
		if(mylist.namelist[i] == NULL) { //this element is empty, we can add it here
			for(j =0; j<MAXNAMES; j++){
				if(tocopyflags[j] == 1) {
					strcpy(mylist.namelist[i], tocopylist->namelist[j]);
					mylist.portlist[i] = tocopylist->portlist[i];
					mylist.totalnames++;
					tocopyflags[j] = 0;
				}	
			}
		}
	}
	return 0;
}

//update my list
static int synclist(struct namesOfDDS *listfrompeer) {
	int i =0, j =0, inlist =0;
	
	for (i =1; i<MAXNAMES; i++){//check if the peers name is alredy in the list or not
		if (strcmp(mylist.namelist[i], listfrompeer->namelist[0]) ==0){
			return 0;
		}
		else {
			inlist =0;
		}
	}
	
	//peers name is not in mylist, so i will add first
	if(inlist ==0){
		for (i =1; i<4; i++){
			if (mylist.namelist[i] == NULL){
				strcpy(mylist.namelist[i], listfrompeer->namelist[0]);
				mylist.totalnames++;
				break;
			}
		}	
	}
	return 0;
}	

//1001010
