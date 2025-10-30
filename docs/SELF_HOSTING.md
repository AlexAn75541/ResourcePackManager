# Self-Hosting Resource Packs

This guide explains how to set up and configure ResourcePackManager to host resource packs on your own server.

## Configuration

1. Open `plugins/ResourcePackManager/config.yml` and set these options:

```yaml
# Enable auto-hosting
autoHost: true

# Enable local server instead of magmaguy.com
useLocalServer: true

# Configure server URL (change this if hosting publicly)
serverUrl: "localhost"  # Use your server's IP or domain if hosting publicly

# Configure server port
serverPort: 50000  # Make sure this port is open in your firewall
```

## Local Hosting

The plugin includes a built-in HTTP server for hosting resource packs. When `useLocalServer` is enabled:

1. Resource packs are stored in `plugins/ResourcePackManager/localserver/packs/`
2. A SQLite database tracks the packs at `plugins/ResourcePackManager/localserver/packs.db`
3. The server runs on the configured port (default: 50000)

## Public Hosting

To make your resource packs accessible to players outside your network:

1. Configure your firewall to allow incoming connections on the configured port
2. Set `serverUrl` to your server's public IP address or domain name
3. If using a domain, set up DNS records to point to your server
4. Consider setting up SSL/TLS for secure connections (requires additional configuration)

## Security Considerations

1. The server doesn't require authentication - anyone who knows the URL can download the packs
2. Monitor disk space usage in the `localserver` directory
3. Consider implementing rate limiting if hosting publicly
4. Backup the `packs.db` file regularly

## Troubleshooting

1. If players can't download packs:
   - Check if the port is open in your firewall
   - Verify the server URL is correct and accessible
   - Check server logs for errors
   - Try accessing the URL directly in a web browser

2. If the server won't start:
   - Check if the port is already in use
   - Verify the plugin has write permissions in its directory
   - Check server logs for detailed error messages

## Support

If you encounter issues:
1. Check the server console for error messages
2. Verify your configuration
3. Test connectivity to the resource pack URL
4. Report issues on the plugin's issue tracker