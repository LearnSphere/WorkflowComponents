
import logging

logger = logging.getLogger(__name__)


class IframeBuilder(object):

    def __init__(self, src, height=1024, width=768):

        self.src = src
        self.height = height
        self.width = width

    def get_document(self):
        output = '<!DOCTYPE html>\n'
        output = output + "<html>\n"
        output = output + "<body>\n"
        output = output + ('<iframe src="%s" width="%i" height="%i"></iframe>\n' % (self.src, self.width, self.height))
        output = output + "</body>\n"
        output = output + "</html>\n"
        return output

