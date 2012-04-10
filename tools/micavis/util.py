import sys, os

_ot_warnings = set()

def onetime_warning(msg):
    if msg in _ot_warnings:
        return
    else:
        _ot_warnings.add(msg)
        print >> sys.stderr, msg
