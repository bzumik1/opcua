#OPC UA demo server
##Docker

- ut -> without authorization
- ph -> name of server -> must be localhost

docker run --rm -it -p 50000:50000 -p 8080:8080 --name opcplc mcr.microsoft.com/iotedge/opc-plc:1.1.6 --ut --ph=localhost --sph --sn=2 --sr=20 --st=uint --fn=2 --fr=1 --ft=uintarray

