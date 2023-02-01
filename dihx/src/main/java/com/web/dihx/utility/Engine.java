package com.web.dihx.utility;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.web.dihx.model.Builder;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.KubeConfig;
import io.kubernetes.client.util.wait.Wait;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;

public class Engine {

    public static void storeBuilderData(Builder builder) {
        /*ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        try {
            objectMapper.writeValue(new File("/home/virag/work/gigaSpace/dihx/builder.yaml"), builder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }*/
    }

    public static void main(String[] args) throws ApiException, IOException {
        String kubeConfigPath = System.getenv("HOME") + "/.kube/config";

        deployKubernetsService(kubeConfigPath);
        getKubernetPods(kubeConfigPath);

    }
    public static void deployKubernetsService(String kubeConfigPath) throws ApiException, IOException {
        ApiClient client = ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new FileReader(kubeConfigPath))).build();
        Configuration.setDefaultApiClient(client);
        AppsV1Api appsV1Api = new AppsV1Api(client);

        String deploymentName = "example-nginx-virag";
        String imageName = "nginx:1.21.6";
        String namespace = "default";

        // Create an example deployment
        V1DeploymentBuilder deploymentBuilder =
                new V1DeploymentBuilder()
                        .withApiVersion("apps/v1")
                        .withKind("Deployment")
                        .withMetadata(new V1ObjectMeta().name(deploymentName).namespace(namespace))
                        .withSpec(
                                new V1DeploymentSpec()
                                        .replicas(1)
                                        .selector(new V1LabelSelector().putMatchLabelsItem("name", deploymentName))
                                        .template(
                                                new V1PodTemplateSpec()
                                                        .metadata(new V1ObjectMeta().putLabelsItem("name", deploymentName))
                                                        .spec(
                                                                new V1PodSpec()
                                                                        .containers(
                                                                                Collections.singletonList(
                                                                                        new V1Container()
                                                                                                .name(deploymentName)
                                                                                                .image(imageName))))));
        appsV1Api.createNamespacedDeployment(
                namespace, deploymentBuilder.build(), null, null, null, null);

        // Wait until example deployment is ready
        Wait.poll(
                Duration.ofSeconds(3),
                Duration.ofSeconds(60),
                () -> {
                    try {
                        System.out.println("Waiting until example deployment is ready...");
                        return appsV1Api
                                .readNamespacedDeployment(deploymentName, namespace, null)
                                .getStatus()
                                .getReadyReplicas()
                                > 0;
                    } catch (ApiException e) {
                        e.printStackTrace();
                        return false;
                    }
                });
        System.out.println("Created example deployment!");
    }
    public static void getKubernetPods(String kubeConfigPath) {
        // loading the out-of-cluster config, a kubeconfig from file-system
        try {
            ApiClient client =
                    ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new FileReader(kubeConfigPath))).build();

            // set the global default api-client to the in-cluster one from above
            Configuration.setDefaultApiClient(client);

            // the CoreV1Api loads default api-client from global configuration.
            CoreV1Api api = new CoreV1Api();
            V1PodList list =
//                    api.listPodForAllNamespaces(null, null, null, null, null, null, null, null, null, null);
                    api.listNamespacedPod("default"
                            , null
                            ,null
                            ,null
                            ,null
                            ,null
                            ,null
                            ,null
                            ,null
                            ,null
                            ,null
                    );
            for (V1Pod item : list.getItems()) {
                System.out.println(item.getMetadata().getName());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }
}
