import logging

def newLogger(name):
    logger = logging.getLogger(__name__ + '.' + name)

    if not logger.hasHandlers():
        lf = logging.Formatter('%(name)s:%(funcName)s %(lineno)d: %(message)s')

        lh = logging.FileHandler('reception.log')
        lh.setFormatter(lf)

        logger.addHandler(lh)
        logger.setLevel(logging.DEBUG)

    return logger

