import pandas as pd
def csvs2df(files):
    list_ = []
    for file_ in files:
        df = pd.read_csv(file_, index_col=None, header=0)
        list_.append(df)

    frame = pd.concat(list_, axis=0, ignore_index=True)
    return frame