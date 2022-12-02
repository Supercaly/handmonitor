#!/bin/sh

# compress wear
cd wear/build/reports
tar -czf wear-lint-report.tar.gz ktlint lint-results-debug.*

cd ../../../

# compress sensorslib
cd sensorslib/build/reports
tar -czf sensorslib-lint-report.tar.gz ktlint lint-results-debug.*

cd ../../../