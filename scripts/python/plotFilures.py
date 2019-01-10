from __future__ import division
import glob
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
from matplotlib import rc

from utiles import csvs2df

fs=10
line_w=1
marker_s=4
face_c='none'
markers=['s', 'x', '+', '^']

def getYlabels(plot) :
    if plot == 1:
        return np.arange(0, 31, 5)
    return np.arange(0, 51, 10)
def plotFailures(dir, sizes, oPath):
    rows = 1
    cols = 2
    index = 1
    n_groups = 3
    bar_width = 0.35
    opacity = 1
    names = ['$\\beta=100$', '$\\beta=1000$']
    fig, ax = plt.subplots(nrows=rows, ncols=cols, figsize=(4, 3))
    plt.subplots_adjust(wspace=0.2, hspace=0.5)
    for s in sizes:
        sb = str(rows) + str(cols) + str(index)
        sb = int(sb)
        ax2 = plt.subplot(sb) #, aspect='equal', adjustable='box-forced')

        benginFiles = glob.glob(dir + "/*." + s + ".bengin/servers/res/summery.csv")
        benginDf = csvs2df(benginFiles)

        byzFiles = glob.glob(dir + "/*." + s + ".byz/servers/res/summery.csv")
        byzDf = csvs2df(byzFiles)

        benginDf['txPsec'] = benginDf['txTotal'] / 60
        byzDf['txPsec'] = byzDf['txTotal'] / 60
        index2 = np.arange(n_groups)


        benginDf = benginDf[['channels', 'txPsec']].groupby(benginDf.channels).mean()
        byzDf = byzDf[['channels', 'txPsec']].groupby(byzDf.channels).mean()

        rects1 = plt.bar(index2, benginDf['txPsec'] / 1000, bar_width,
                         alpha=opacity, color='gray', hatch='xx',
                        label='Benign fault')
        rects2 = plt.bar(index2 + bar_width, byzDf['txPsec'] / 1000, bar_width,
                         alpha=opacity,
                         label='Byzantine fault')
        ax2.set_xticks(index2 + bar_width / 2)
        ax2.set_xticklabels(('1', '5', '10'), fontsize=fs)
        plt.yticks(getYlabels(index), fontsize=fs)
        plt.title(names[index-1], fontsize=fs)
        plt.grid(True)
        index += 1

    # plt.rc('text', usetex=True)
    # plt.rc('font', family='serif')
    leg = fig.legend([],  # The line objects
                     labels=[r"$F_{omission}$", r"$F_{byz}$"],  # The labels for each line
                     loc="lower center",  # Position of legend
                     # borderaxespad=0.01,  # Small spacing around legend box
                     fontsize=fs,
                     ncol=4,
                     frameon=False,
                     bbox_to_anchor=(0.5, -0.05),
                     )
    # plt.set_xlabel('channels')
    # plt.set_ylabel('Throughput (KTxs / sec)')

    # ax.legend()
    #
    # fig.tight_layout()

    fig.text(0.51, 0.08, "$\\omega$", ha="center", va="center", fontsize=fs)
    fig.text(0.03, 0.5, "Throughput ($\\frac{KTxs}{sec}$)", ha="center", va="center", rotation=90, fontsize=fs)
    fig.tight_layout(rect=[0.03, 0.04, 1, 1])
    for d in oPath:
        plt.savefig(d + '/failures.pdf')
        plt.savefig(d + '/failures')

if __name__ == "__main__":
    plotFailures("/home/yoni/toy/res_fail", ['100', '1000'],
                 ["/home/yoni/toy/figures", "/home/yoni/Dropbox/paper/draws"])
