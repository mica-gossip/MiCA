#!/usr/bin/python
import sys
sys.path.insert(0,"../..")
logdir = "../../../mica_log"

from micavis.gui import main

args = sys.argv[:1] + [logdir]

main(args)

