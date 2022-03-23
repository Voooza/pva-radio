@echo off

pushd pva-radio-fe
call npm run build
robocopy public ../pva-radio-be/resources/public /S /is /it
popd

pushd pva-radio-be
call lein uberjar
popd


