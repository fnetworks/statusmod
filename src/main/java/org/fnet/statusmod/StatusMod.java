package org.fnet.statusmod;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.config.Configuration;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.relauncher.Side;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.Level;

@Mod(modid = "statusmod", version = "1.0", name = "Status Mod", acceptableRemoteVersions = "*", acceptedMinecraftVersions = "1.7.10")
public class StatusMod {
	private final Gson gson = new Gson();
	private URL webhookURL = null;

	@EventHandler
	public void preInit(FMLPreInitializationEvent e) throws MalformedURLException {
		Configuration config = new Configuration(e.getSuggestedConfigurationFile());
		String webhookURL = config.getString("webhookUrl", "Discord", "", "The discord webhook URL");
		if (config.hasChanged())
			config.save();
		if (webhookURL == null || webhookURL.isEmpty())
			FMLLog.info("Discord webhook not configured, disabling status reports");
		else
			this.webhookURL = new URL(webhookURL);
	}

	@EventHandler
	public void serverStarted(FMLServerStartedEvent e) {
		if (e.getSide() == Side.SERVER && webhookURL != null) {
			try {
				sendMessage("Server is online!");
			} catch (Exception ex) {
				FMLLog.log(Level.ERROR, ex, "Failed to send discord message");
			}
		}
	}

	@EventHandler
	public void serverStopped(FMLServerStoppingEvent e) {
		if (e.getSide() == Side.SERVER && webhookURL != null) {
			try {
				sendMessage("Server went offline");
			} catch (Exception ex) {
				FMLLog.log(Level.ERROR, ex, "Failed to send discord message");
			}
		}
	}

	private void sendMessage(String msg) throws IOException, URISyntaxException {
		HttpPost post = new HttpPost(webhookURL.toURI());
		CloseableHttpClient client = HttpClients.createDefault();
		try {
			final SimpleDiscordMessage dmsg = new SimpleDiscordMessage(msg);
			StringEntity requestEntity = new StringEntity(gson.toJson(dmsg), ContentType.APPLICATION_JSON);
			post.setEntity(requestEntity);
			post.addHeader("Content-Type", "application/json");

			final CloseableHttpResponse response = client.execute(post);

			if (response.getEntity() == null || response.getEntity().getContentLength() == 0)
				return;

			JsonObject obj = gson.fromJson(EntityUtils.toString(response.getEntity()), JsonObject.class);
			String error = obj.get("message").getAsString();
			throw new IOException("Discord Message push failed: " + error);
		} finally {
			client.close();
		}
	}
}
