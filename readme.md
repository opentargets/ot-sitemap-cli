# Open Targets Platform Sitemap

This repository is used to generate a [SEO 'sitemap'](https://developers.google.com/search/docs/advanced/sitemaps/overview) for the [Open Targets Platform](https://beta.targetvalidation.org/). 

This program reads data from Google Big Query to generate the sitemaps, and optionally saves the outputs to Google 
Cloud Storage. 

The program generates sitemaps for:
  - disease_association
  - disease_profile
  - drug_profile
  - target_association
  - target_profile

And creates an index referencing each. 

In cases where there are more than 50k entries the sitemap is split into chunks.

## Running locally 

This script requires Google Cloud authentication to be set up to execute Big Query queries. 

If authentication is already set up you don't need to do anything, as Google Application Default Credentials (ADC)
resolution mechanism will find the appropriate credentials in the default location. 

You can also use environment variables or Google Secret Manager. 

Create a jar file with the `sbt assembly` command from the directory containing 
the `build.sbt` file. A jar file will be generated in the `/target/scala-2.12/` directory.

To run the jar use the following command to display the help text: 

`java -jar target/scala-2.12/ot-sitemap.jar --help`

### Help text:

```
Generate SEO sitemaps for open targets platform. 0.1
Usage: ot-sitemap [options] <bigQueryTable> [<bigQueryProject>]

  <bigQueryTable>          BigQuery table to query for results, eg. platform_21_02
  <bigQueryProject>        GCP project containing BQ tables. Default: open-targets-eu-dev
  -o, --output-dir <value>
                           
                           Directory to save generated sitemaps into. Selected directory can be either an absolute or relative path or a 
                           gcloud storage bucket location.
                           
                           The default directory is the current working directory. 
                           
                           If a storage bucket is selected then you must have authorisation set up on the machine running the program to 
                           access those resources.
  --help                   prints this usage text

```

### Example usage: 

To query from BQ `platform_21_04` and save results to `gs://open-targets-data-releases/21.04/metadata/sitemaps` 
within the domain of project `open-targets-prod`:

```
java -jar target/scala-2.12/ot-sitemap.jar \
--output-dir=gs://open-targets-data-releases/21.04/metadata/sitemaps
platform_21_04
open-targets-prod
```

If no output directory is given sitemaps will be generated in the local directory. If an output directory is given 
but either the Bucket is invalid or cloud storage is unavailable the outputs will be written to local disk. 

Note, the project hosting Big Query is the project which the output will be written to. 

### Using environment variable

`export GOOGLE_APPLICATION_CREDENTIALS="<path to secret key json file>"`

### Using Secret Manager

1. If you don't already have authorisation set-up, create a new Key on an appropriate service
account and save that json file locally. 
2. Add that key to [GCP Secret Manager](https://cloud.google.com/secret-manager) 
3. Run `gcloud auth login --update-adc` to configure local account to use Secret Manager keys.

## Dependency documentation

- [Scala XML](https://github.com/scala/scala-xml/wiki)
- [Scopt](https://github.com/scopt/scopt): Used to generate the command line interface.
- [GCP Java APIs](https://cloud.google.com/java/docs/reference)
  - [ResourceManager](https://googleapis.dev/java/google-cloud-resourcemanager/latest/index.html) - Centrally manage all your projects, IAM, and resource containers.
  - [Storage](https://googleapis.dev/java/google-cloud-storage/latest/index.html) - GCP storage