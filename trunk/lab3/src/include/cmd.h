/**
 * DDS - COMMAND
 * -------------
 * by Gokul Soundararajan
 *
 * The command set used by dds
 *
 **/

#ifndef __CMD_H__
#define __CMD_H__

typedef enum dds_cmd {
  DDS_GET=0,
  DDS_PUT,
  DDS_DELETE,
  DDS_LISTBUCKET,
  DDS_KEYINFO,
  DDS_GOSSIP,
  DD_GETNAMES,
  DDS_MAX /* max command -- keep this here */
} dds_cmd;

static char *dds_verbs [DDS_MAX] = {
  "GET",
  "PUT",
  "DELETE",
  "LISTBUCKET",
  "KEYINFO",
  "GOSSIP",
  "GETNAMES"
};

#endif
