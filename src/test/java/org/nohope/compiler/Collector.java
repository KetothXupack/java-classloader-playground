package org.nohope.compiler;

import org.objectweb.asm.commons.Remapper;

import java.util.Set;

/**
 * @author <a href="mailto:ketoth.xupack@gmail.com">Ketoth Xupack</a>
 * @since 2014-04-17 20:33
 */
public class Collector extends Remapper {

    private final Set<String> classNames;
    private final String prefix;

    public Collector(final Set<String> classNames, final String prefix){
        this.classNames = classNames;
        this.prefix = prefix;
    }

    @Override
    public String mapDesc(final String desc){
        if(desc.startsWith("L")){
            this.addType(desc.substring(1, desc.length() - 1));
        }
        return super.mapDesc(desc);
    }

    @Override
    public String[] mapTypes(final String[] types){
        for(final String type : types){
            this.addType(type);
        }
        return super.mapTypes(types);
    }

    private void addType(final String type){
        final String className = type;// = type.replace('/', '.');
        if(className.startsWith(this.prefix)) {
            //try {
                //Class.forName(className);
            //} catch (ClassNotFoundException e) {
                this.classNames.add(className);
            //}
        }
    }

    @Override
    public String mapType(final String type){
        this.addType(type);
        return type;
    }
}
