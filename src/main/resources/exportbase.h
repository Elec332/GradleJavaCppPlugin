#ifndef TOREPLACE_EXPORT_H
#define TOREPLACE_EXPORT_H

#ifdef TOREPLACE_STATIC_DEFINE
#  define TOREPLACE_EXPORT
#  define TOREPLACE_NO_EXPORT
#else
#  ifndef TOREPLACE_EXPORT
#    ifdef TOREPLACE_EXPORTS //We are building this library
#      ifndef _MSC_VER
#        define TOREPLACE_EXPORT __attribute__((visibility("default")))
#      else
#        define TOREPLACE_EXPORT __declspec(dllexport)
#      endif
#    else //We are using this library
#      ifndef _MSC_VER
#        define TOREPLACE_EXPORT __attribute__((visibility("default")))
#      else
#        define TOREPLACE_EXPORT __declspec(dllimport)
#      endif
#    endif
#  endif

#  ifndef TOREPLACE_NO_EXPORT
#    ifndef _MSC_VER
#      define TOREPLACE_NO_EXPORT __attribute__((visibility("hidden")))
#    else
#      define TOREPLACE_NO_EXPORT
#    endif
#  endif
#endif

#ifndef TOREPLACE_DEPRECATED
#  ifndef _MSC_VER
#    define TOREPLACE_DEPRECATED __attribute__((__deprecated__))
#  else
#    define TOREPLACE_DEPRECATED __declspec(deprecated)
#  endif
#endif

#ifndef TOREPLACE_DEPRECATED_EXPORT
#  define TOREPLACE_DEPRECATED_EXPORT TOREPLACE_EXPORT TOREPLACE_DEPRECATED
#endif

#ifndef TOREPLACE_DEPRECATED_NO_EXPORT
#  define TOREPLACE_DEPRECATED_NO_EXPORT TOREPLACE_NO_EXPORT TOREPLACE_DEPRECATED
#endif

#endif /* TOREPLACE_EXPORT_H */
