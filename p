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
   TC0:
        image: ctoy:0.1
        container_name: TC_0
        environment:
        - CID=0
        - SID=0
        - TXS=100
        - TEST=p
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
        - TEST=p
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
        - TEST=p
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
        - TEST=p
        volumes:
        - /tmp/dtoy/ccurr:/tmp/JToy
        - /tmp/dtoy/configurations/client:/JToy/bin/src/main/resources
        networks:
            toy_net:
                ipv4_address: 172.18.0.7
   TC1:
        image: ctoy:0.1
        container_name: TC_1
        environment:
        - CID=1
        - SID=1
        - TXS=100
        - TEST=p
        volumes:
        - /tmp/dtoy/ccurr:/tmp/JToy
        - /tmp/dtoy/configurations/client:/JToy/bin/src/main/resources
        networks:
            toy_net:
                ipv4_address: 172.18.0.8
   TC2:
        image: ctoy:0.1
        container_name: TC_2
        environment:
        - CID=2
        - SID=2
        - TXS=100
        - TEST=p
        volumes:
        - /tmp/dtoy/ccurr:/tmp/JToy
        - /tmp/dtoy/configurations/client:/JToy/bin/src/main/resources
        networks:
            toy_net:
                ipv4_address: 172.18.0.9
   TC3:
        image: ctoy:0.1
        container_name: TC_3
        environment:
        - CID=3
        - SID=3
        - TXS=100
        - TEST=p
        volumes:
        - /tmp/dtoy/ccurr:/tmp/JToy
        - /tmp/dtoy/configurations/client:/JToy/bin/src/main/resources
        networks:
            toy_net:
                ipv4_address: 172.18.0.10
