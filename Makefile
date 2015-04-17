#export CXX = clang++

programs = libosmscout \
           libosmscout-import \
           libosmscout-map \
           libosmscout-map-svg \
           libosmscout-map-cairo \
           libosmscout-map-agg \
           libosmscout-map-qt \
           libosmscout-map-agg \
           libosmscout-map-opengl \
           Import \
           DumpData \
           Demos \
           OSMScout2 \
           StyleEditor \
           Tests

lol = libosmscout \
           libosmscout-import \
           libosmscout-map \
           libosmscout-map-cairo

all:
	@for x in $(programs); do \
	  if [ -d $$x ]; then \
	    (cd $$x && $(MAKE)); \
	  fi \
	done

full:
	@for x in $(programs); do \
	  if [ -d $$x ]; then \
	    echo Building $$x...; \
	    (cd $$x && ./autogen.sh && ./configure && $(MAKE)); \
	  fi \
	done

full-install:
	@for x in $(lol); do \
	  if [ -d $$x ]; then \
	    echo Building $$x...; \
	    (cd $$x && ./autogen.sh && ./configure && $(MAKE) && sudo $(MAKE) install); \
	  fi \
	done

clean:
	@for x in $(programs); do\
	  if [ -d $$x ]; then \
	    (cd $$x && $(MAKE) clean); \
	  fi \
	done

dist:
	@for x in $(programs); do\
	  if [ -d $$x ]; then \
	    (cd $$x && $(MAKE) dist); \
	  fi \
	done

distclean:
	@for x in $(programs); do\
	  if [ -d $$x ]; then \
	    (cd $$x && $(MAKE) distclean); \
	  fi \
	done
