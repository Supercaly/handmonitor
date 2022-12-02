#!/bin/sh

# compress wear
cd wear/build/reports
tar -czf wear-test-report.tar.gz tests

cd ../../../

# compress sensorslib
cd sensorslib/build/reports
tar -czf sensorslib-test-report.tar.gz tests

cd ../../../