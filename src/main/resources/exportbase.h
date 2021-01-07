#ifndef REPLACEME_EXPORT_H
#define REPLACEME_EXPORT_H

#ifdef REPLACEME_STATIC_DEFINE
#  define REPLACEME_EXPORT
#  define REPLACEME_NO_EXPORT
#else
#  ifndef REPLACEME_EXPORT
#    ifdef REPLACEME_CREATE_EXPORTS //We are building this library
#      ifdef _MSC_VER
#        define REPLACEME_EXPORT __declspec(dllexport)
#      else
#        define REPLACEME_EXPORT __attribute__((visibility("default")))
#      endif
#    else //We are using this library
#      ifdef _MSC_VER
#        define REPLACEME_EXPORT __declspec(dllimport)
#      else
#        define REPLACEME_EXPORT __attribute__((visibility("default")))
#      endif
#    endif
#  endif

#  ifndef REPLACEME_NO_EXPORT
#    ifdef _MSC_VER
#      define REPLACEME_NO_EXPORT
#    else
#      define REPLACEME_NO_EXPORT __attribute__((visibility("hidden")))
#    endif
#  endif
#endif

#ifndef REPLACEME_DEPRECATED
#  ifdef _MSC_VER
#    define REPLACEME_DEPRECATED __declspec(deprecated)
#  else
#    define REPLACEME_DEPRECATED __attribute__((__deprecated__))
#  endif
#endif

#ifndef REPLACEME_DEPRECATED_EXPORT
#  define REPLACEME_DEPRECATED_EXPORT REPLACEME_EXPORT REPLACEME_DEPRECATED
#endif

#ifndef REPLACEME_DEPRECATED_NO_EXPORT
#  define REPLACEME_DEPRECATED_NO_EXPORT REPLACEME_NO_EXPORT REPLACEME_DEPRECATED
#endif

#endif /* REPLACEME_EXPORT_H */
