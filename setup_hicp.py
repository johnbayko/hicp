from setuptools import setup, find_packages

with open("python/hicp/README.rst", "r") as fh:
    long_description = fh.read()

setup(
    name='hicp',
    version='0.0.1',
    description='HICP interface for Python',
    long_description=long_description,
    long_description_content_type='text/x-rst',
    url='https://github.com/johnbayko/hicp',
    author='John Bayko',
    author_email='jbayko@sasktel.net',
    packages=find_packages(where="python", include=["hicp"]),
    package_dir={'':'python'},
    license='MIT',
)
