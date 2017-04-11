import unittest
import argparse
import sys
import os

from tests_common import  NewAlgorithmTests

basedir = os.path.abspath(os.path.dirname(__file__))

def load_tests(loader, tests, pattern):

    tests.addTests(loader.discover(basedir, pattern='test_*.py'))
    return tests

if __name__ == '__main__':

    parser = argparse.ArgumentParser(description = 'run algorithm python test')
    parser.add_argument('--repo', help = 'use sample image files from local git repository (path to folder)')
    args, other = parser.parse_known_args()
    #print("Local repo path:", args.repo)
    print "======================================"
    NewAlgorithmTests.repoPath = args.repo
    unit_argv = [sys.argv[0]] + other
    unittest.main(argv = unit_argv)

