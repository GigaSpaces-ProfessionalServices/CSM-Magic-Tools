package com.web.dihx.utility;

//import com.amazonaws.ClientConfiguration;
//import com.amazonaws.DefaultRequest;
//import com.amazonaws.Protocol;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1NamespaceList;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.eks.EksClient;

import java.net.URISyntaxException;

class AwsEngineInstance {
    public void generateToken() throws ApiException {
        String accessKey = "AKIATCDDMI7JBIA5HI5C";
        String secretKey = "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";
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(
                accessKey,
                secretKey);
        EksClient eksClient = EksClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();
        ApiClient client = Configuration.getDefaultApiClient();

        CoreV1Api api = new CoreV1Api(client);
        V1NamespaceList list = api.listNamespace("",false,"","","", new Integer(1), "", "", 1, false);
        System.out.println(list);
    }
//    public String generateEksToken(String clusterName, String region, String accessKey, String secretKey) throws URISyntaxException {
//
//        DefaultRequest defaultRequest = new DefaultRequest<>(
//                new GetCallerIdentityRequest(), "sts");
//        URI uri = new URI("https", "sts.amazonaws.com", null, null);
//        System.out.println("1");
//        defaultRequest.setResourcePath("/");
//        defaultRequest.setEndpoint(uri);
//        defaultRequest.setHttpMethod(HttpMethodName.GET);
//        defaultRequest.addParameter("Action", "GetCallerIdentity");
//        defaultRequest.addParameter("Version", "2011-06-15");
//        defaultRequest.addHeader("x-k8s-aws-id", clusterName);
//        System.out.println("2");
//        BasicAWSCredentials basicCredentials = new BasicAWSCredentials(accessKey, secretKey);
//        System.out.println("3");
//        AWSStaticCredentialsProvider credentials = new AWSStaticCredentialsProvider(basicCredentials);
//        System.out.println("4");
//        Signer signer = SignerFactory.createSigner(SignerFactory.VERSION_FOUR_SIGNER,
//                new SignerParams("sts", region));
//        System.out.println("5");
//        AWSSecurityTokenServiceClient stsClient = (AWSSecurityTokenServiceClient) AWSSecurityTokenServiceClientBuilder
//                .standard().withRegion(region).withCredentials(credentials).build();
//        System.out.println("6");
//        SignerProvider signerProvider = new DefaultSignerProvider(stsClient, signer);
//        System.out.println("7");
//        PresignerParams presignerParams = new PresignerParams(uri, credentials, signerProvider,
//                SdkClock.STANDARD);
//        System.out.println("8");
//        PresignerFacade presignerFacade = new PresignerFacade(presignerParams);
//        System.out.println("9");
//        URL url = presignerFacade.presign(defaultRequest, new Date(System.currentTimeMillis() + 60000));
//        System.out.println("10");
//        String encodedUrl = Base64.getUrlEncoder().withoutPadding().encodeToString(url.toString().getBytes());
//        return "k8s-aws-v1." + encodedUrl;
//    }
}
public class AwsEngine {
    /*public static void main(String[] args) {
        String accessKey = "AKIATCDDMI7JBIA5HI5C";
        String secretKey = "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";
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        ClientConfiguration clientConfig = new ClientConfiguration ();
        clientConfig.setProtocol (Protocol.HTTPS);
        clientConfig.setMaxErrorRetry (PredefinedRetryPolicies.DEFAULT_MAX_ERROR_RETRY);
        clientConfig.setRetryPolicy (new RetryPolicy(PredefinedRetryPolicies.DEFAULT_RETRY_CONDITION,
                PredefinedRetryPolicies.DEFAULT_BACKOFF_STRATEGY, PredefinedRetryPolicies.DEFAULT_MAX_ERROR_RETRY, false));

        AmazonEKS amazonEKS = AmazonEKSClientBuilder.standard ()
                .withClientConfiguration (clientConfig)
                .withCredentials (new AWSStaticCredentialsProvider(credentials))
                .withRegion ("us-east-1") //replace your region name
                .build ();
        CreateClusterResult eksCluster = amazonEKS.createCluster (
                new CreateClusterRequest().withName ("cluster-name")
                        .withRoleArn("arn:aws:iam::210661820370:role/eks-access")
                //with other param
        );
        System.out.println(eksCluster.toString());
    }*/


    /*public static void main(String[] args) {
        try{
            JSch jsch=new JSch();

            String user = "ec2-user";
            String host = "3.141.197.142";
            int port = 22;
            File directory = new File(".");
//            String privateKey = directory.getCanonicalPath() + File.separator + "pem file path";
            String privateKey = "/home/virag/work/gigaSpace/DIHX_project/ps-share.pem";

            jsch.addIdentity(privateKey, "password");
            System.out.println("identity added ");

            Session session = jsch.getSession(user, host, port);
            System.out.println("session created.");
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
//            config.put("PreferredAuthentications", "password");
            session.setConfig(config);
            session.connect();

            Channel channel=session.openChannel("shell");
            channel.setInputStream(System.in);
            channel.setOutputStream(System.out);
            channel.connect(3*1000);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }*/

//    public static void main(String[] args) throws URISyntaxException {
//        URI uri = new URI("https", "sts.amazonaws.com", null, null);
//
//        String region = "us-east2";
//        String accessKey = "AKIATCDDMI7JBIA5HI5C";
//        String secretKey = "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";
//        AWSCredentials
//                credentials = new BasicAWSCredentials(accessKey, secretKey);
//        AWSCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(credentials);
//        Signer signer = SignerFactory.createSigner(SignerFactory.VERSION_FOUR_SIGNER,
//                new SignerParams("sts", region));
//        AWSSecurityTokenServiceClient stsClient = (AWSSecurityTokenServiceClient) AWSSecurityTokenServiceClientBuilder
//                .standard().withRegion(region).withCredentials(credentialsProvider).build();
//        SignerProvider signerProvider = new DefaultSignerProvider(stsClient, signer);
//
//        PresignerParams presignerParams = new PresignerParams(uri, credentialsProvider, signerProvider,
//                SdkClock.STANDARD);
//        PresignerFacade presignerFacade = new PresignerFacade(presignerParams);
//        DefaultRequest defaultRequest = new DefaultRequest("test");
//        defaultRequest.setResourcePath("/");
//
//        defaultRequest.setEndpoint(uri);
//
//        defaultRequest.setHttpMethod(HttpMethodName.GET);
//
//        defaultRequest.addParameter("Action", "GetCallerIdentity");
//
//        defaultRequest.addParameter("Version", "2011-06-15");
//
//        defaultRequest.addHeader("x-k8s-aws-id", "dihx1");
//
//        URL url = presignerFacade.presign(defaultRequest, new Date(System.currentTimeMillis() + 60000));
//
//        String encodedUrl = Base64.getUrlEncoder().withoutPadding().encodeToString(url.toString().getBytes());
//    }



    public static void main(String[] args) throws URISyntaxException, ApiException {
        AwsEngineInstance awsEngineInstance = new AwsEngineInstance();
        awsEngineInstance.generateToken();
        if(true) {
            return;
        }
//        String url = "https://CE47E7A60C5C557EF514C70819F9B834.gr7.us-east-2.eks.amazonaws.com";
//        String accessKey = "AKIATCDDMI7JBIA5HI5C";
//        String secretKey = "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";
//        String token = awsEngineInstance.generateEksToken("dihx1", "us-east-2", accessKey, secretKey);
//        System.out.println("11");
//        ApiClient client = Config.fromToken(url, token);
//        System.out.println("12");
    }

}