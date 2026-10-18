# Local build dependencies

Place a legally obtained `SlashBladeResharped-1.20.1-1.1.23.jar` here before building. Third-party JARs are not distributed by this repository. Java 17 is required; set JAVA_HOME on your own machine.

The default build targets Minecraft 1.20.1 / Forge 47.3.22 / SlashBlade Resharped 1.1.23. To test 1.8.62, place `SlashBladeResharped-1.20.1-1.8.62.jar` here and pass `-PslashbladeVersion=1.8.62`.

Optional Boss tests require the matching local dependency JARs named `cataclysm-local.jar`, `lionfishapi-local.jar`, `mowziesmobs-local.jar`, `geckolib-local.jar`, `curios-local.jar`, `goety-local.jar`, and `revelation-local.jar`. Ordinary builds do not require them. Test reports must state the actual Boss count; an absent optional dependency is not a compatibility pass.
