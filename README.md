## Starter for Play with Scala projects

This is a blank [Play framework](http://www.playframework.com) project that will connect to any [prismic.io](https://prismic.io) repository. It uses the prismic.io Scala developement kit, and provide a few helpers to integrate with the Play framework.

### How to start?

Edit the `conf/application.conf` file to make the application point to the correct repository:

```
# Prismic.io
# ~~~~~

# API endpoint
prismic.api="https://lesbonneschoses.prismic.io/api"

# If specified this token is used for all "guest" requests
# prismic.token="xxx"

# OAuth2 configuration
# prismic.clientId="xxxxxx"
# prismic.clientSecret="xxxxxx"
```

Run your play application using either the `play run` or the `sbt run` command and open your browser at http://localhost:9000/

### Licence

This software is licensed under the Apache 2 license, quoted below.

Copyright 2013 Zengularity (http://www.zengularity.com).

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.