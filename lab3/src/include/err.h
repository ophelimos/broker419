/**
 * DDS - ERROR 
 * -----------
 * by Gokul Soundararajan
 *
 * A basic exception-like construct
 * for my code.
 *
 **/

#ifndef __ERR_H__
#define __ERR_H__


#define DIE_IF_EQUAL( ACTUAL, EXPECTED, MSG, LABEL, ERRED) {	\
    if( ACTUAL == EXPECTED ) { \
      fprintf(stderr, "[ERROR]: %s!\n", MSG); \
      fflush(stderr);				\
      *ERRED = 1;				\
      goto LABEL;				\
    }						\
  }

#define DIE_IF_NOT_EQUAL( ACTUAL, EXPECTED, MSG, LABEL, ERRED) {	\
    if( ACTUAL != EXPECTED ) { \
      fprintf(stderr, "[ERROR]: %s!\n", MSG); \
      fflush(stderr);				\
      *ERRED = 1;				\
      goto LABEL;				\
    }						\
  }


#endif
