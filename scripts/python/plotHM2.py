from __future__ import division
import glob
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

from utiles import csvs2df

fs=14

line_w=1
marker_s=4
face_c='none'
markers=['s', 'x', '+', '^']

def plotNormal(dirs, oPath):

    xlab=['1','5','10']
    names=['$n=4$', '$n=7$', '$n=10$']
    labels = ['A-B', 'B-C', 'C-D', 'D-E']
    realLabels = ['BP2T','BP2D','BP2DL','HP2T','HP2D','HP2DL','HT2D','HD2DL']
    rows = 1
    cols = 3
    index = 1
    n = 0
    fig, ax = plt.subplots(nrows=rows, ncols=cols)
    plt.subplots_adjust(wspace=0.1, hspace=0.5)
    beta = [1000]
    workers = [1, 5, 10]

    for d in dirs:

        files = glob.glob(d + "/summeries/*.csv")
        df = csvs2df(files)
        m = 0
        for b in beta:
            sb = str(rows) + str(cols) + str(index)
            sb = int(sb)
            ax2 = plt.subplot(sb)
            bdata=df[df.txInBlock == b]
            ndata = []
            for w in workers:
                wdata=bdata[bdata.workers == w]
                ndata += [
                    [
                    (wdata['BP2T'].mean() - wdata['HP2T'].mean()) / wdata['BP2DL'].mean()
                    , wdata['HP2T'].mean() / wdata['BP2DL'].mean()
                    , wdata['HT2D'].mean()/ wdata['BP2DL'].mean()
                    , wdata['HD2DL'].mean()/ wdata['BP2DL'].mean()
                    ]
                ]
            im = ax2.imshow(zip(*ndata), cmap='Reds')
            ax2.set_xticks(np.arange(len(xlab)))
            ax2.set_yticks(np.arange(len(labels)))

            ax2.set_xticklabels(xlab, fontsize=fs)
            if index == 1:

                ax2.set_yticklabels(labels, fontsize=fs)
            else:
                plt.yticks([])



            ax2.set_xticks(np.arange(len(xlab) + 1) - .5, minor=True)
            ax2.set_yticks(np.arange(len(labels) + 1) - .5, minor=True)

            for edge, spine in ax2.spines.items():
                spine.set_visible(False)

            ax2.grid(which="minor", color="black", linestyle='-', linewidth=3)
            ax2.tick_params(which="minor", bottom=False, left=False)
            plt.setp(ax2.get_xticklabels(),
                     rotation_mode="anchor", fontsize=fs)
            for i in range(len(xlab)):
                for j in range(len(labels)):
                    clr='black'
                    if round(ndata[i][j], 2) >= 0.45:
                        clr='white'
                    text = ax2.text(i, j, str(round(ndata[i][j], 2)),
                                   ha="center", va="center", color=clr, fontsize=fs)
            plt.title(names[index-1], fontsize=fs)
            index+=1
    fig.text(0.5, 0.22, "$\\omega$", ha="center", fontsize=fs, va="center")
    # cbaxes = fig.add_axes([0, 0.1, 0.03, 0.8])
    # cbar = plt.colorbar(im, cax=cbaxes, fraction=0.0258, pad=0.04)




    # cbar.ax.set_location("bottom")
    # cbar.ax.set_ylabel("Relative execution time", rotation=0, va="bottom", fontsize=fs)
    # cbar = plt.colorbar(im, fraction=0.28, pad=0.15, orientation='horizontal')
    # fig.text(0.53, 0, "Relative execution time", ha="center", fontsize=fs, va="center")
    # for edge, spine in ax.spines.items():
    #     spine.set_visible(False)
    #
    # ax.grid(which="minor", color="black", linestyle='-', linewidth=3)
    # ax.tick_params(which="minor", bottom=False, left=False)
    # plt.setp(ax.get_xticklabels(),
    #          rotation_mode="anchor", fontsize=fs)
    # for i in range(len(chan)):
    #     for j in range(len(labels)):
    #         clr='black'
    #         if round(normData[i][j], 2) >= 0.45:
    #             clr='white'
    #         text = ax.text(i, j, str(round(normData[i][j], 2)),
    #                        ha="center", va="center", color=clr, fontsize=fs)
    # fig.text(0.53, 0.08, "$n(\\omega)$", ha="center", fontsize=fs, va="center")
    fig.tight_layout(rect=[0.02, 0.065, 1, 1.02])
    for d in oPath:
        plt.savefig(d + '/heatmap2.pdf', bbox_inches='tight')
        plt.savefig(d + '/heatmap2', bbox_inches='tight')




def plotHM(dirs, oPath):
    plotNormal(dirs, oPath)

if __name__ == "__main__":
    plotHM([
                "/home/yoni/toy/m5/correct/4"
        ,
        "/home/yoni/toy/m5/correct/7"
        ,
        "/home/yoni/toy/m5/correct/10"
             ]
             , ["/home/yoni/toy/figures", "/home/yoni/Dropbox/paper/figures"])