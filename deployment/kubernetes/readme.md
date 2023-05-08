# Deploying  dicomstore service in  Kubernetes
Tested on **Docker Desktop** with kubernetes
## Steps
- Edit **ingress-nginx-controller** service add  tcp listen entries for tcp traffic
  ```shell
  kubectl edit svc ingress-nginx-controller -n ingress-nginx
  ```
  Add below entries into **ingress-nginx-controller** service
  ```yaml
  ports:
    - appProtocol: proxied-tcp-4040
      name: proxied-tcp-4040
      port: 4040
      protocol: TCP
      targetPort: 4040
  ```
- Edit **ingress-nginx-controller** deployment add command line parameter to use 'tcp-services' configmap
  ```shell
  'kubectl edit deploy ingress-nginx-controller -n ingress-nginx'
  ```
  ```yaml
       - args:
          - /nginx-ingress-controller
          - .. other entries
          - --tcp-services-configmap=ingress-nginx/tcp-services
  ```
More informaton of adding tcp ingress is @ https://kubernetes.github.io/ingress-nginx/user-guide/exposing-tcp-udp-services/
