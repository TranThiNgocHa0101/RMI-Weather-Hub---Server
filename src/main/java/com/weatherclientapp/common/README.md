# RMI Weather Monitor - Server

RMI Weather Monitor Server is the backend component of the JavaFX-based weather monitoring system. Its main responsibility is to fetch weather data from external APIs, process the information, and provide it to connected clients via RMI (Remote Method Invocation). This allows client applications to receive real-time, multi-day weather forecasts efficiently and reliably.  

The server handles multiple key tasks, including:  
- Fetching data from a weather API and updating it in real-time.  
- Serving temperature and rainfall data to clients for visualization (LineCharts and BarCharts).  
- Supporting communication with multiple clients through RMI and UDP protocols, including chat functionality.  
- Enabling Admin and Co-Admin functionality for managing users and monitoring statistics.  
- Sending automatic disaster alerts and login notification emails to users.  

This architecture ensures that clients do not need to directly access external APIs, improving security, scalability, and data consistency. The server acts as the central hub, collecting, processing, and distributing weather information to all connected clients.

## Demo & Client
- Demo: [https://reccloud.com/u/13p3c9u](https://reccloud.com/u/13p3c9u)  
- Client Repository: [https://github.com/TranThiNgocHa0101/RMI-Weather-Hub---Client.git](https://github.com/TranThiNgocHa0101/RMI-Weather-Hub---Client.git)

## Features
- Real-time multi-day weather forecast processing
- Temperature LineCharts & Rainfall BarCharts distribution
- Admin/Co-Admin user management and statistics
- Disaster alerts & automatic login emails
- RMI/UDP integration and client communication
### Instructions:
-  Run the server, then run the client.
