#!/bin/bash

mvn -e clean deploy -Dgpg.keyname=$1
