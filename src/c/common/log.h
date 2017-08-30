#ifndef __LOG_H
#define __LOG_H
/**
 * Motion log level.
 **/
#define  SLS_LOG_DEBUG                        0x0001

/**
 * Stuff which is only useful for algorithm developers.
 **/
#define  SLS_LOG_INFO                         0x0002

/**
 * Something somehow does not look correct
 **/
#define  SLS_LOG_WARNING                      0x0003

/**
 * Something went wrong and cannot losslessly be recovered
 **/
#define  SLS_LOG_ERROR                        0x0004

/**
 * Something went wrong and recovery is not possible
 **/
#define  SLS_LOG_FATAL                        0x0005

#endif
