#!/usr/bin/env zsh

docker run -e "ACCEPT_EULA=Y" -e "MSSQL_SA_PASSWORD=qzpKmbjsVw2FsWSqQ468UCM4NoZuvNPk" -p 1433:1433 -d mcr.microsoft.com/mssql/server:2022-latest