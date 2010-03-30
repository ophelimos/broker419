/**
 * DDS - SERVE
 * -----------
 * by Gokul Soundararajan
 *
 * This provides the web server front end
 * to DDS.
 * 
 **/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <hermes.h>
#include <mongoose.h>
#include <dds.h>

/* Global Variables */
/* ----------------------------------------------------------------- */
static int web_port, dds_port;
static char *dds_server;

/* Function Headers */
/* ----------------------------------------------------------------- */
/* Mongoose Adapters */
static void __mg_handle( struct mg_connection *conn, const struct mg_request_info *ri, void *data );

/* Main */
/* ----------------------------------------------------------------- */

int main(int argc, char **argv) {

  struct mg_context *ctx = NULL;

  /* parse command line arguments */
  if( argc == 4 ) {
    web_port = atoi( argv[1] );
    dds_server = argv[2];
    dds_port = atoi( argv[3] );
  } else {
    fprintf(stderr, "USAGE: %s <web_port> <dds_server> <dds_port>\n", argv[0] );
    fflush(stderr);
    return -1;
  }

  /* start up mongoose */
  ctx = mg_start();
  mg_set_option(ctx, "root", "/tmp" );
  mg_set_option(ctx, "ports", argv[1] );
  mg_bind_to_uri(ctx, "/*", &__mg_handle, NULL );

  /* wait for shutdown */
  while(1) {
    sleep(1);
  }

  mg_stop(ctx);

  return 0;

}

/* Function Implementation */
/* ----------------------------------------------------------------- */

static void __mg_handle( struct mg_connection *conn, const struct mg_request_info *ri, void *data ) {

  int erred = 0;
  char *bucket_name, *key_name;

  hms_msg *request = NULL;
  hms_msg *reply  = NULL;

  /* Extract Bucket/Key from URI */
  fprintf(stdout, "Got URI[%s]\n", ri->uri );
  char *sep = "/", *brk, *input = ri->uri;
  bucket_name = strtok_r(input, sep, &brk );
  key_name = strtok_r(NULL, sep, &brk );
  fprintf(stdout, "Extracted Bucket:[%s] Key:[%s]\n", bucket_name, key_name );

  DIE_IF_EQUAL( bucket_name, NULL, "Invalid bucket name", cleanup_handle, &erred );
  DIE_IF_EQUAL( key_name, NULL, "Invalid key name", cleanup_handle, &erred );

  /* Connect to DDS */
  hms_ops ops;
  hms_endpoint *dds_conn = NULL;
  int conn_id = hms_endpoint_connect( dds_server, dds_port );
  dds_conn = hms_endpoint_init( conn_id, ops );

  /* Send Request */
  request = hms_msg_create();
  DIE_IF_EQUAL( request, NULL, "Could not create message", cleanup_handle, &erred );
  DIE_IF_EQUAL( hms_msg_set_verb( request, "GET" ), -1, "Could not set verb", cleanup_handle, &erred);
  DIE_IF_EQUAL( hms_msg_add_named_header( request, "Bucket", bucket_name ), -1, 
		"Could not add Bucket to header", cleanup_handle, &erred );
  DIE_IF_EQUAL( hms_msg_add_named_header( request, "Key", key_name ), -1,
		"Could not add Key to header", cleanup_handle, &erred );
  DIE_IF_EQUAL( hms_endpoint_send_msg ( dds_conn, request ), -1, 
		"Could not send msg to DDS", cleanup_handle, &erred);

  /* Get Reply */
  DIE_IF_EQUAL( hms_endpoint_recv_msg( dds_conn, &reply ), -1,
		"Could not receive reply from DDS", cleanup_handle, &erred );
  
  /* Extract body */
  char *body; int body_len;
  DIE_IF_EQUAL( hms_msg_get_body( reply, &body, &body_len ), -1,
		"Could not extract body from message", cleanup_handle, &erred );
  DIE_IF_EQUAL( body_len, 0, "Content-Length is ZERO", cleanup_handle, &erred );

  /* Send reply */
  mg_printf(conn, "%s", "HTTP/1.1 200 OK\r\n" );
  mg_printf(conn, "%s", "Content-Type: text/txt\r\n");
  mg_printf(conn, "%s %d\r\n", "Content-Length:", body_len);
  mg_printf(conn, "%s", "Connection: close\r\n\r\n" );
  mg_write(conn, body, body_len );

  cleanup_handle:
  /* Send Error */
  if(erred) {
    mg_printf(conn, "%s", "HTTP/1.1 404 NOT FOUND\r\n" );
    mg_printf(conn, "%s", "Content-Type: text/html\r\n");
    mg_printf(conn, "%s", "Connection: close\r\n\r\n" );
  }

  /* Clean-up */
  if(body) free(body); body = NULL;
  if(request) hms_msg_destroy( request );
  if(reply) hms_msg_destroy( reply );
  

} /* end __mg_handle() */
