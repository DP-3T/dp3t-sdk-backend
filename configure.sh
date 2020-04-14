#!/bin/bash

if [ cargo ]; then
    echo "We found cargo lets install rusty swagger"
    cargo install rusty-swagger
else
    echo "Installing rust"
    curl https://sh.rustup.rs -sSf | sh
fi