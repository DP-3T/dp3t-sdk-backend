<!-- Version taken from https://raw.githubusercontent.com/fastlane/fastlane/master/CONTRIBUTING.md and adapted to our needs-->
# Contributing to dpppt-backend-sdk

## I want to report a problem or ask a question

Before submitting a new GitHub issue, please make sure to

- Check out the README pages on [this repo](https://github.com/DP-3T/dp3t-sdk-backend)
- Search for [existing GitHub issues](https://github.com/DP-3T/dp3t-sdk-backend/issues)

If the above doesn't help, please [submit an issue](https://github.com/DP-3T/dp3t-sdk-backend/issues) on GitHub and provide information about your setup. If you feel like something is not working as it should, please provide the `logback` output.

**Note**: If you want to report a regression (something that has worked before, but broke with a new release), please mark your issue title as such using `[Regression] Your title here`. This enables us to quickly detect and fix regressions.


## I want to contribute to DP3T

Before you submit a new pull-request, please check the following few things:

- [ ] My pull-request is not just about code-style (if so please open an issue, where we can discuss about it).
- [ ] My pull-request does not introduce a plethora of new dependencies
- [ ] My pull-request introduces new features and/or fixes issues (please reference an issue if there exist one).
- [ ] If my pull-request changes tooling, I added some major arguments, on why this tooling is better.

## Why did my issue/PR get closed?

We currently are preparing to go live soon. Hence, we try to keep the changes to a minimum. Since we also heavily rely on public pentesting, we also don't want to introduce new dependencies for no reason.

Changes, which only concern different tooling should have arguments attached. We are using tools and setups we are used to, and if there is no major reason (security issues, performance issues) we probably won't change it.

## Above All, Thanks for Your Contributions

Thank you for reading to the end, and for taking the time to contribute to the project! If you include the 🔑 emoji at the top of the body of your issue or pull request, we'll know that you've given this your full attention and are doing your best to help!

## License

This project is licensed under the terms of the MPL 2 license. See the [LICENSE](LICENSE) file.
