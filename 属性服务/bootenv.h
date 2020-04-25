
#ifndef _INIT_BOOTENV_H
#define _INIT_BOOTENV_H

int is_bootenv_varible(const char* prop_name);
int init_bootenv_varibles();
int update_bootenv_varible(const char* name, const char* value);
#if BOOT_ARGS_CHECK
void 	check_boot_args();
#endif
#endif

