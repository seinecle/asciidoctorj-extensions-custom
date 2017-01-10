/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.mojopublishtomedium;

/**
 *
 * @author LEVALLOIS
 */
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.json.JSONObject;

/**
 * Says "Hi" to the user.
 *
 */
@Mojo(name = "publishtomedium")
public class PublishToMedium extends AbstractMojo {

    @Parameter(property = "publish2medium.token", defaultValue = "2d1de7c1949b8fc8f75d53f541cc476e636da8747948eab3bcaf9188bc5f12fc5")
    private String token;

    @Parameter(property = "medium.publish", defaultValue = "false")
    private String publish;

    @Parameter(property = "publishtomedium.fileName", defaultValue = "")
    private String fileName;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            if (!Boolean.parseBoolean(publish)) {
                return;
            }
            getLog().info("in the maven plugin to publish on Medium");
            CloseableHttpClient client = HttpClients.createDefault();

            HttpPost httpPost = new HttpPost("https://api.medium.com/v1/users/17eadf7b6d3d14558aeebc97bf4b8630b9e33b30e65293db63eb7cb8ff4d192ee/posts");
            httpPost.setHeader("Authorization", "Bearer " + token);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Accept", "application/json");

            JSONObject obj = new JSONObject();

            String path = fileName;
            System.out.println("path to html file to be published to Medium: ");
            System.out.println(path);

            BufferedReader br = new BufferedReader(new FileReader(path));
            String line;
            String title = "placeholder";
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                System.out.println(line);
                sb.append(line);
                sb.append("\n");
                if (line.contains("h1")) {
                    title = line.replace("<h1>", "").replace("</h1>", "");
                }
            }
            obj.put("title", "title2");
            obj.put("contentFormat", "html");
            obj.put("content", sb.toString());
            obj.put("canonicalUrl", "https://medium.com/@seinecle/" + URLEncoder.encode(title, "UTF-8"));
            obj.put("publishStatus", "draft");

            StringEntity entity = new StringEntity(obj.toString());
            httpPost.setEntity(entity);

            CloseableHttpResponse response = client.execute(httpPost);

            String resp = EntityUtils.toString(response.getEntity());
            System.out.println("response: " + resp);
        } catch (IOException ex) {
            Logger.getLogger(PublishToMedium.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
