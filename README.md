# CMC Jenkins Library

Reusable bits of code used in CMC Jenkins pipelines.

## Usage

To use the library in a pipeline declare it somewhere at the top of the pipeline file:

```
@Library('CMC')
```

If you are developing new library features and want to test them you can specify the branch to use with:

```
@Library('CMC@new-library-feature')
```

`new-library-feature` is the name of a branch on this repository.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE.txt) file for details.
