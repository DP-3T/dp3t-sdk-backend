#!/bin/bash

if [ cargo ]; then
    echo "We found cargo lets install rusty swagger"
    cargo install rusty-swagger
else
    echo "Installing rust"
    curl https://sh.rustup.rs -sSf | sh
    source ~/.cargo/env
    cargo install rusty-swagger
fi