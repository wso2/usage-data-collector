# WSO2 Usage Data Collector

[![Build Status](https://img.shields.io/jenkins/build?jobUrl=https%3A%2F%2Fwso2.org%2Fjenkins%2Fview%2Fproducts%2Fjob%2Fproducts%2Fjob%2Fproduct-apim%2F)](<To be added>)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![stackoverflow](https://img.shields.io/badge/stackoverflow-wso2mi-orange)](https://stackoverflow.com/tags/wso2-am/)
[![stackoverflow](https://img.shields.io/badge/stackoverflow-wso2am-orange)](https://stackoverflow.com/tags/wso2-micro-integrator/)

---

# Overview

This directory contains the **WSO2 Usage Data Collector** - a component of the Consumption Tracker system designed to effectively track and record product consumption data within the **WSO2 Micro Integrator (MI)** environment.

### Description

During the initial sale of a product (or multiple products), agreements are made regarding the number of cores or transaction volumes. However, after deployment, there is currently no proper visibility into whether customers are consuming more than their allocated limits.

Without an existing mechanism to **record or track consumption** (cores, transaction volumes, or automation runtime), we rely solely on customer-provided information.

The **Usage Data Collector** addresses this challenge by providing a **system to record product consumption data** and **generate usage reports** when required.


---

### Architecture

The consumption tracking system consists of three main components deployed across MI nodes:

<img width="800" alt="Overall Architecture" src="./docs/counter_highlevel_arch.png" />

## MI-Side Components

The system introduces three main components on the MI side:

- **Data Collector** - Gathers usage and deployment information
- **Data Receiver** - Receives and secures data before forwarding to endpoints


### Data Collector

The **Usage Data Collector** is the component housed in this repository. 

The broader Consumption Tracker system includes two types of data collectors:

| Subcomponent | Description | Frequency / Trigger | Location |
|--------------|-------------|---------------------|--------|
| **Deployment Data Collector** | Collects environment-level data such as CPU core count, JDK version, OS, and update level. Publish usage-data, deployment info to the necessary location(API or/and DB)  | Daily/ EventBased / On restart | collectors/org.wso2.carbon.deployment.data.collector |
| **Usage Data Collector** | Captures the number of transactions per hour processed by MI nodes. Give the publishing DB info to the Deployment Data Collector | Hourly | collectors/org.wso2.carbon.usage.data.collector.mi |

> **Note**: The **Usage Data Collector** component will be product specific and The Deployment Data Collector is a component designed for reuse across multiple WSO2 products.


### Data Access Options

- **Local Storage**: If data is stored locally, it can be exported using CLI commands when needed.
- **Public API**: If data is published to the public API, WSO2 can directly monitor customer usage

---

### Data Receiver
The **Data Receiver** component:

- Secures incoming data from MI nodes
- Forwards data to designated endpoints for processing and storage
- Publishes data based on the available destination:
  - **Choreo-hosted WSO2 API** (for cloud-based monitoring) [Priority]
  - **Internal Database** (for customers preferring on-premise storage)

## Installation

1. Copy the generated JAR to your MI installation:
   ```bash
   cp counter/target/org.wso2.carbon.usage.data.collector.mi-0.1.0.jar <MI_HOME>/lib/
   ```

2. Update the `deployment.toml` configuration as shown above

3. Restart the MI server

## Building from Source

### Prerequisites

- Java 8 or higher
- Maven 3.6+
- Git

### Build Steps

```bash
# Clone the repository
git clone https://github.com/wso2/usage-data-collector.git
cd usage-data-collector/collectors/org.wso2.carbon.usage.data.collector.mi

# Build the project
mvn clean install

# The JAR will be created at:
# counter/target/org.wso2.carbon.usage.data.collector.mi-0.1.0.jar
```

---

## Project Structure

```
usage-data-collector/
├── collectors/
│   └── org.wso2.carbon.usage.data.collector.mi/
│       ├── src/
│       │   └── main/
│       │       ├── java/                  # Java source files
│       │       └── resources/             # Resource files
│       ├── pom.xml                        # Maven build file
│       └── README.md                      # This README file
└── README.md                             # Root README file
```

---

## Data Collected

### Usage Data (Hourly)

- Transaction count per hour

---

## Contributing

We welcome contributions from the community! To contribute:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Guidelines

- Follow existing code style and conventions
- Update documentation as needed
- Ensure compatibility with Apache 2.0 License

---

## Issues and Support

Please report issues at: [GitHub Issues](https://github.com/wso2/product-micro-integrator/issues)

For WSO2 product support, visit:
- [WSO2 Support Portal](https://support.wso2.com/)
- [WSO2 Community](https://wso2.com/community/)

---

## License

This project is licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0).

---

## Additional Resources

- [WSO2 Micro Integrator Documentation](https://mi.docs.wso2.com/)
- [WSO2 API Manager Documentation](https://apim.docs.wso2.com/)
- [Apache Synapse](https://synapse.apache.org/)

---

(c) 2025, [WSO2 LLC](http://www.wso2.org/). All Rights Reserved.