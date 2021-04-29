# Open Targets Platform Sitemap

This repository is used to generate a [SEO 'sitemap'](https://developers.google.com/search/docs/advanced/sitemaps/overview) for the [Open Targets Platform](https://beta.targetvalidation.org/). 

## Running locally 

This script requires Google Cloud authentication to be set up to execute Big Query queries. 

If authentication is already set up you don't need to do anything, as Google Application Default Credentials (ADC)
resolution mechanism will find the appropriate credentials in the default location. 

You can also use environment variables or Google Secret Manager. 

### Using environment variable

`export GOOGLE_APPLICATION_CREDENTIALS="<path to secret key json file>"`

### Using Secret Manager

1. If you don't already have authorisation set-up, create a new Key on an appropriate service
account and save that json file locally. 
2. Add that key to [GCP Secret Manager](https://cloud.google.com/secret-manager) 
3. Run `gcloud auth login --update-adc` to configure local account to use Secret Manager keys.

## Dependency documentation

- [Scopt](https://github.com/scopt/scopt): Used to generate the command line interface.
- [GCP Java APIs](https://cloud.google.com/java/docs/reference)
  - [ResourceManager](https://googleapis.dev/java/google-cloud-resourcemanager/latest/index.html) - Centrally manage all your projects, IAM, and resource containers.
  - [Storage](https://googleapis.dev/java/google-cloud-storage/latest/index.html) - GCP storage