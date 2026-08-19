cmake_minimum_required(VERSION 3.22.1)
project(discord_bridge)

find_package(discord_partner_sdk REQUIRED CONFIG)

add_library(discord_bridge SHARED
    discord_bridge.cpp
)

target_link_libraries(discord_bridge
    discord_partner_sdk::discord_partner_sdk
    log
)

target_compile_features(discord_bridge PRIVATE cxx_std_20)
