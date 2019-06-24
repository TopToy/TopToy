   TC0:
        image: ctoy:0.1
        container_name: TC_0
        environment:
        - CID=0
        - SID=0
        - TXS=100
        - TEST=0
        volumes:
        - /tmp/dtoy/ccurr:/tmp/JToy
        - /tmp/dtoy/configurations/client:/JToy/bin/src/main/resources
        networks:
            toy_net:
                ipv4_address: 172.18.0.4
   TC0:
        image: ctoy:0.1
        container_name: TC_0
        environment:
        - CID=0
        - SID=0
        - TXS=100
        - TEST=0
        volumes:
        - /tmp/dtoy/ccurr:/tmp/JToy
        - /tmp/dtoy/configurations/client:/JToy/bin/src/main/resources
        networks:
            toy_net:
                ipv4_address: 172.18.0.4
   TC0:
        image: ctoy:0.1
        container_name: TC_0
        environment:
        - CID=0
        - SID=0
        - TXS=100
        - TEST=
        volumes:
        - /tmp/dtoy/ccurr:/tmp/JToy
        - /tmp/dtoy/configurations/client:/JToy/bin/src/main/resources
        networks:
            toy_net:
                ipv4_address: 172.18.0.4
