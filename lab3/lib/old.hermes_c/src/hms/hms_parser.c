/**
 * HERMES
 * ------
 * by Daniel Lupei and Gokul Soundararajan
 *
 * A C implementation of the Hermes protocol
 * - contains the packet parser
 *
 **/

#include <hermes.h>
#include <hermes_internal.h>
#include <errno.h>
#include <ctype.h>
#include <strings.h>

/* Function prototypes */
/* ---------------------------------------------------- */
static void __hms_str_strip( char ** str );
static int __hms_parse_read_header( int fd, char *buffer, int buf_len );
static int __hms_parse_header( hms_msg *msg, char *buffer, int len, int *body_len );
static int __hms_parse_verb_line( hms_msg *msg, char *line );
static int __hms_parse_named_header( hms_msg *msg, char *line, int *body_len );
static int __hms_parse_read_body( int fd, hms_msg *msg, int body_len );

/* Implementation */
/* ---------------------------------------------------- */

hms_msg *hms_msg_parse( int fd , int max_hdr_len ) {

  char *buffer = NULL; int len = 0;
  buffer = (char *) malloc( max_hdr_len + 1); len=max_hdr_len;
  buffer[max_hdr_len] = '\0';
  hms_msg *msg = NULL;

  /* Read header */
  int hdr_len = __hms_parse_read_header( fd, buffer, len);
  //printf("hdr: len: %d data: |%s|\n", hdr_len, buffer);
  if( hdr_len > 2 ) {
    int erred = HMS_FALSE; int body_len = 0;
    msg = hms_msg_create();
    if(!__hms_parse_header(msg, buffer, hdr_len, &body_len) ) {
      if(body_len) {
	if( __hms_parse_read_body( fd, msg , body_len ) != 0 )
	  erred = HMS_TRUE;
      }
    } else { erred = HMS_TRUE; }
    /* If erred, then clean up */
    if(erred && msg) { hms_msg_destroy( msg ); msg = NULL; }
  }

  free(buffer); buffer = NULL; len = 0;

  return msg;

} /* end hms_msg_parse() */

/* Helper Functions */
/* ---------------------------------------------------- */

static int __hms_parse_read_header( int fd, char *buffer, int buf_len ) {

  /* Check input */
  hms_assert_not_equals( __FILE__, __LINE__, (int) NULL, (int) buffer );
  hms_assert_not_equals( __FILE__, __LINE__, (int) 0, (int) buf_len );

  /* Read until ".\n" or buf_len */
  unsigned hdr_len = 0, dot_seen = HMS_FALSE, is_done = HMS_FALSE;
  char *dot_ptr = NULL;
  while( (hdr_len < buf_len) ) {
      char c;
      if( read( fd, &c, 1) == 1 ) {
	if( c == '\r' ) { c = ' '; }
	buffer[hdr_len++] = c;
	if( c == '.' ) { dot_seen = HMS_TRUE; dot_ptr = &buffer[hdr_len-1]; continue; }
	if(dot_seen && c == '\n' ) { is_done = HMS_TRUE; break; }
	if(dot_seen && (c != ' ' && c != '\r' && c != '\n') ) { dot_seen = HMS_FALSE; dot_ptr = NULL; }
      } else { break; }

  } /* end while() */

  //printf("buf:|%s|\n", buffer); fflush(stdout);

  /* Put "\0" where "." exists */
  if(dot_ptr) *dot_ptr = '\0';

  /* Put "\0" at the end if theres room */
  if(hdr_len < buf_len) buffer[hdr_len]='\0';

  return (is_done == HMS_TRUE) ? hdr_len : -1;

} /* end __hms_parse_read_header() */

static int __hms_parse_header( hms_msg *msg, char *buffer, int len, int *body_len ) {

  char *line, *line_save, *line_sep = "\n";
  int count = 0;
  int erred = HMS_FALSE;

  for( line=strtok_r(buffer, line_sep, &line_save);
       line;
       line=strtok_r(NULL, line_sep, &line_save) ) {

    if( count == 0) {
      if(__hms_parse_verb_line(msg,line)) { 
	erred=HMS_TRUE; break;
      }
    } else {
      if(__hms_parse_named_header( msg, line, body_len )) {
	erred = HMS_TRUE; break;
      }
    }

    count++;

  } /* end for loop */

  return ( erred == HMS_FALSE ) ? 0 : -1;

} /* end __hms_parse_header() */

static int __hms_parse_verb_line( hms_msg *msg, char *line ) {

  char *word, *word_save, *word_sep = " \t";
  int count = 0;
  int found_verb = HMS_FALSE;

  for( word=strtok_r(line, word_sep, &word_save);
       word;
       word=strtok_r(NULL, word_sep, &word_save)) {

    /* verb */
    if( count == 0 ) { 
      hms_msg_set_verb( msg, word ); found_verb = HMS_TRUE; 
      //printf("verb:|%s|\n", word);
    } else {
      hms_msg_add_header( msg, word ); 
      //printf("uhdr:|%s|\n", word);
    }

    count++;

  } /* end for loop */

  return found_verb ? 0 : -1;

} /* end __hms_parse_verb_line() */

static int __hms_parse_named_header( hms_msg *msg, char *line, int *body_len ) {

  char hdr_sep = ':';
  char *key, *val;
  key = line; val = strchr(line, hdr_sep );

  if(val == NULL) { return -1; } else { *val='\0';  val++; }
  __hms_str_strip( &key ); __hms_str_strip( &val );

  /* check for content-length */
  if( strcasecmp(key, "Content-Length" ) == 0) {
    *body_len = (int) strtol(val, (char **)NULL, 10);
    if(*body_len == 0) return -1;
  }

  hms_msg_add_named_header( msg, key, val );
  //printf("nhdr: key: |%s| val:|%s| \n", key, val);

  return 0;

} /* end __hms_parse_named_header() */

static void __hms_str_strip( char ** str ) {
  char* last;	
  while( isspace( **str ) ) { (*str)++; }
  last = *str + strlen( *str ) - 1;
  while( isspace( *last ) ){ *last = 0;last--;}

} /* end __hms_str_strip() */

static int __hms_parse_read_body( int fd, hms_msg *msg, int body_len ) {

  char *buffer = NULL;
  int read_in = 0, erred = HMS_FALSE;
  buffer = malloc( body_len + 1 );

  while( read_in < body_len ) {
    int sz = read( fd, buffer, (body_len - read_in) );
    if( sz <= 0 ) { erred = HMS_TRUE; break; }
    else { read_in += sz; }
  }

  if(erred == HMS_FALSE) {
    assert(read_in == body_len );
    //printf("body:|%s|\n", buffer );
    hms_msg_set_body( msg, buffer, body_len );
  }

  if(buffer) { free(buffer); buffer = NULL; }

  return (erred == HMS_TRUE) ? -1 : 0; 

} /* end __hms_parse_read_body() */
