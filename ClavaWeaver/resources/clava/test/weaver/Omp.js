aspectdef test

    select omp end
    apply
        println("OMP:" + $omp.name);
        println("OMP CONTENT:" + $omp.content);
        println("OMP TARGET:" + $omp.target.code);
    end

end